package controllers

import javax.inject.Inject

import models.{ Person, SystemUser }
import models.http._
import play.api.Logger
import play.api.cache.SyncCacheApi
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Result }
import services.AlexaIntentService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DialogController @Inject() (cache: SyncCacheApi, service: AlexaIntentService) extends Controller with RequestProcessor {
  val logger = Logger(this.getClass)
  val INTENT_NAME = "BetaIntent"
  val FIRST_NAME_SLOT = "firstName"
  val LAST_NAME_SLOT = "lastName"
  val PERSON_NAME_SLOT = "personName"
  val DEPARTMENT_SLOT = "department"
  val NO_ECARD_SENT_BYE_MSG = "No eCard sent. Bye."
  val SENDER_SYS_USER_ID = "2961698"

  def alpha = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    val alexaRequest = request.body.validate[AlexaRequest].get
    alexaRequest.request.`type` match {
      case "IntentRequest" =>
        alexaRequest.request.dialogState match {
          case Some("STARTED") =>
            handleStarted(alexaRequest)
          case Some("IN_PROGRESS") =>
            logger.info("IN_PROGRESS")
            handleInProgressUtterance(alexaRequest)
          case _ =>
            handleCompleted(alexaRequest)
        }
    }
  }

  def handleStarted(alexaRequest: AlexaRequest): Future[Result] = {
    clearCache(4)
    handleInProgressUtterance(alexaRequest)
  }

  def handleInProgressUtterance(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        if (intent.slots.get(DEPARTMENT_SLOT).isEmpty ||
          (intent.slots.get(DEPARTMENT_SLOT).isDefined && intent.slots.get(DEPARTMENT_SLOT).get.value.isEmpty)) {
          val personName = intent.slots.get(PERSON_NAME_SLOT).get.value.get
          //Call elastic search to get names and departments
          val r = for {
            searchResults <- getSearchResults(personName)
          } yield {
            if (searchResults.isEmpty) {
              noSearchResults(personName)
            } else if (searchResults.length == 1) {
              oneSearchResult(searchResults.head)
            } else {
              moreThanOneSearchResult(personName, searchResults)
            }
          }
          r.flatten
        } else if (intent.confirmationStatus.isEmpty || intent.confirmationStatus.get.equals("NONE")) {
          val deptNum = intent.slots.get(DEPARTMENT_SLOT).get.value.get
          askForIntentConfirmation(deptNum)
        } else if (intent.confirmationStatus.isDefined && intent.confirmationStatus.get.equals("CONFIRMED")) {
          handleCompleted(alexaRequest)
        } else {
          logger.error("Did not match any if else")
          quit(NO_ECARD_SENT_BYE_MSG)
        }
      case None =>
        logger.error("No intent for person name utterance")
        quit(NO_ECARD_SENT_BYE_MSG)
    }
  }

  //Call search here.
  def getSearchResults(name: String): Future[Seq[Person]] = {
    val nameTuple = service.extractRecipientFirstAndLastName(name)
    for {
      sysUsers <- service.searchForRecipients(SENDER_SYS_USER_ID, nameTuple._1, nameTuple._2)
    } yield {
      val uptoThree = sysUsers.take(3)
      //TODO need to get the first name. last name and department from the sysUsers
      uptoThree.map(s => Person("Bryan", "Cannon", "InfoTech", s.systemUserId, s.customerId, s.employeeId))
    }

    /*
    val random = (Math.random() * 10 % 4).toInt
    logger.info("********************* random result count = " + random)
    random match {
      case 0 => Seq()
      case 1 => Seq(Person("Bryan", "Cannon", "Human Resources"))
      case 2 => Seq(Person("Bryan", "Cannon", "Human Resources"), Person("Bryant", "Canyon", "InfoTech"))
      case 3 => Seq(Person("Bryan", "Cannon", "Human Resources"), Person("Bryant", "Canyon", "InfoTech"), Person("Brent", "Canton", "Labs"))
    }*/
  }

  def noSearchResults(name: String): Future[Result] = {
    logger.info("Could not find anyone with the name " + name)
    quit("Could not find anyone with the name " + name)
  }

  def oneSearchResult(searchResult: Person): Future[Result] = {
    cache.set("1", searchResult)
    askForIntentConfirmation("1")
  }

  def moreThanOneSearchResult(name: String, searchResults: Seq[Person]): Future[Result] = {
    searchResults.foldLeft(1)((i, person) => {
      cache.set(i.toString, person)
      i + 1
    })

    val dName = AlexaDirectiveSlot("personName", Some("NONE"), Some(name))
    val dept = AlexaDirectiveSlot("department", Some("NONE"), None)
    val slots = Map() + ("personName" -> dName) + ("department" -> dept)
    val updatedIntent = AlexaUpdatedIntent(INTENT_NAME, "NONE", slots)
    val directives = Seq(AlexaDirective("Dialog.ElicitSlot", Some("department"), Some(updatedIntent)))
    val outTxt = buildOutputTxtMoreThanOneSearchResult(searchResults)
    val outputSpeech = AlexaOutputSpeech("PlainText", outTxt)
    val respType = AlexaDirectiveResponseType(false, Some(outputSpeech), directives)
    val resp = Json.toJson(AlexaDirectiveResponse("1.0", Map(), respType))
    logger.info(resp.toString)
    Future(Ok(resp))
  }

  def buildOutputTxtMoreThanOneSearchResult(searchResults: Seq[Person]): String = {
    var txt = "Is the person "
    for ((person, j) <- searchResults.zipWithIndex) {
      if (j == 0) {
        txt = txt + (j + 1) + " " + person.firstName + " " + person.lastName + " from " + person.department
      } else {
        txt = txt + ", or " + (j + 1) + " " + person.firstName + " " + person.lastName + " from " + person.department
      }
    }
    txt + ", or " + (searchResults.length + 1) + " None of these? Select a number."
  }

  def askForIntentConfirmation(deptNum: String): Future[Result] = {
    cache.get[Person](deptNum) match {
      case Some(cachedObj) =>
        val dName = AlexaDirectiveSlot("personName", Some("NONE"), Some(cachedObj.firstName + " " + cachedObj.lastName))
        val dept = AlexaDirectiveSlot("department", Some("NONE"), Some(deptNum))
        val slots = Map() + ("personName" -> dName) + ("department" -> dept)
        val updatedIntent = AlexaUpdatedIntent(INTENT_NAME, "NONE", slots)
        val directives = Seq(AlexaDirective("Dialog.ConfirmIntent", None, Some(updatedIntent)))
        val outputSpeech = AlexaOutputSpeech("PlainText", "I will send an eCard to " + cachedObj.firstName + " " + cachedObj.lastName + " from " + cachedObj.department + ". Is that OK?")
        val respType = AlexaDirectiveResponseType(false, Some(outputSpeech), directives)
        val resp = Json.toJson(AlexaDirectiveResponse("1.0", Map(), respType))
        logger.info(resp.toString)
        Future(Ok(resp))
      case None =>
        logger.error("Unable to retrieve " + deptNum + " from cache")
        quit(NO_ECARD_SENT_BYE_MSG)
    }
  }

  def handleCompleted(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        val deptSlot = intent.slots.get(DEPARTMENT_SLOT).getOrElse(AlexaSlot("department", Some("No Department")))
        val cacheObj = cache.get[Person](deptSlot.value.get)
        cacheObj match {
          case Some(p) =>
            //Send eCard here
            for {
              result <- service.sendECard(SENDER_SYS_USER_ID, p.systemUserId.toString, p.customerId)
            } yield {
              val responseQuote = "ECard sent to " + p.firstName + " " + p.lastName + " from " + p.department +
                ". " + getQuirkyEndPhrase()
              val outputSpeech = AlexaOutputSpeech("PlainText", responseQuote)
              val card = AlexaCard("Simple", "ECard", responseQuote)
              val reprompt = AlexaReprompt(outputSpeech)
              val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
              val resp = Json.toJson(AlexaResponse("1.0", Map(), alexaResponseType, true))
              logger.info(resp.toString)
              Ok(resp)
            }
          case None =>
            logger.warn(deptSlot.value.get + " Not found in cache")
            quit(NO_ECARD_SENT_BYE_MSG)
        }
      case None =>
        logger.error("No intent for COMPLETED")
        quit(NO_ECARD_SENT_BYE_MSG)
    }
  }

  def getQuirkyEndPhrase(): String = {
    val random = (Math.random() * 100 % 10).toInt
    random match {
      case 0 => "Cowabunga!"
      case 1 => "Im feeling it!"
      case 2 => "Yum. That was so good!"
      case 3 => "Ding ding ding ding ding!"
      case 4 => "Appreciatologist supremo!"
      case 5 => "Just like milk and cookies!"
      case 6 => "Hubba hubba hubba hubba hubba!"
      case 7 => "Gelatto all around!"
      case 8 => "Its Frebo time!"
      case _ => "Lets do yoga!"
    }
  }

  def quit(responseQuote: String): Future[Result] = {
    clearCache(4)
    val outputSpeech = AlexaOutputSpeech("PlainText", responseQuote)
    val card = AlexaCard("Simple", "ECard", responseQuote)
    val reprompt = AlexaReprompt(outputSpeech)
    val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
    val resp = Json.toJson(AlexaResponse("1.0", Map(), alexaResponseType, true))
    logger.info(resp.toString)
    Future(Ok(resp))
  }

  def clearCache(size: Int): Unit = {
    for (i <- 1 to size) {
      cache.remove(String.valueOf(i))
    }
  }
}
