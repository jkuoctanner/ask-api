package controllers

import javax.inject.Inject

import models.Person
import models.http._
import play.api.Logger
import play.api.cache.{ SyncCacheApi }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Result }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DialogController @Inject() (cache: SyncCacheApi) extends Controller with RequestProcessor {
  val logger = Logger(this.getClass)
  val INTENT_NAME = "BetaIntent"
  val FIRST_NAME_SLOT = "firstName"
  val LAST_NAME_SLOT = "lastName"
  val PERSON_NAME_SLOT = "personName"
  val DEPARTMENT_SLOT = "department"

  //
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
    clearCache()
    handleInProgressUtterance(alexaRequest)
  }

  def handleInProgressUtterance(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        if (intent.slots.get(DEPARTMENT_SLOT).isEmpty ||
          (intent.slots.get(DEPARTMENT_SLOT).isDefined && intent.slots.get(DEPARTMENT_SLOT).get.value.isEmpty)) {
          val personName = intent.slots.get(PERSON_NAME_SLOT).get.value.get
          //TODO Call elastic search to get names and departments
          val searchResults = getSearchResults(personName)
          if (searchResults.isEmpty) {
            noSearchResults(personName)
          } else if (searchResults.length == 1) {
            oneSearchResult(searchResults.head)
          } else {
            moreThanOneSearchResult(personName, searchResults)
          }
        } else if (intent.confirmationStatus.isEmpty || intent.confirmationStatus.get.equals("NONE")) {
          val deptNum = intent.slots.get(DEPARTMENT_SLOT).get.value.get
          askForIntentConfirmation(deptNum)
        } else if (intent.confirmationStatus.isDefined && intent.confirmationStatus.get.equals("CONFIRMED")) {
          handleCompleted(alexaRequest)
        } else {
          quit("Error so No ECard sent")
        }
      case None =>
        logger.error("No intent for person name utterance")
        quit("Error so No ECard sent")
    }
  }

  //TODO Call search here.
  def getSearchResults(name: String): Seq[Person] = {
    val random = (Math.random() * 10 % 4).toInt
    logger.info("********************* random = " + random)
    random match {
      case 0 => Seq(Person("Bryan", "Cannon", "Human Resources"))
      case 1 => Seq(Person("Bryan", "Cannon", "Human Resources"))
      case 2 => Seq(Person("Bryan", "Cannon", "Human Resources"), Person("Bryan", "Cannon", "InfoTech"))
      case 3 => Seq(Person("Bryan", "Cannon", "Human Resources"), Person("Bryan", "Cannon", "InfoTech"), Person("Bryan", "Cannon", "Labs"))
    }
  }

  def noSearchResults(name: String): Future[Result] = {
    quit("Could not find anyone with the name " + name)
  }

  def oneSearchResult(searchResult: Person): Future[Result] = {
    cache.set("1", searchResult)
    askForIntentConfirmation("1")
  }

  def moreThanOneSearchResult(name: String, searchResults: Seq[Person]): Future[Result] = {
    var i = 1
    searchResults.foreach(p => {
      cache.set(i.toString, p)
      i = i + 1
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
    var j = 1
    var txt = "Is the person from "
    searchResults.foreach(p => {
      if (j == 1) {
        txt = txt + j + " " + p.department
      } else {
        txt = txt + ", or " + j + " " + p.department
      }
      j = j + 1
    })
    txt = txt + ", or 4 None of these?"
    logger.info("output txt = " + txt)
    txt
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
        quit("I do not know who to send the eCard to. Bye.")
    }
  }

  def handleCompleted(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        val nameSlot = intent.slots.get(PERSON_NAME_SLOT).getOrElse(AlexaSlot("personName", Some("No Name")))
        val deptSlot = intent.slots.get(DEPARTMENT_SLOT).getOrElse(AlexaSlot("department", Some("No Department")))

        val responseQuote = "ECard sent to " + nameSlot.value.get + " working in " + deptSlot.value.get
        val outputSpeech = AlexaOutputSpeech("PlainText", responseQuote)
        val card = AlexaCard("Simple", "ECard", responseQuote)
        val reprompt = AlexaReprompt(outputSpeech)
        val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
        val resp = Json.toJson(AlexaResponse("1.0", Map(), alexaResponseType, true))
        logger.info(resp.toString)
        Future(Ok(resp))
      case None =>
        logger.error("No intent for COMPLETED")
        quit("Error so No ECard sent")
    }
  }

  def quit(responseQuote: String): Future[Result] = {
    clearCache()
    val responseQuote = "Error so No ECard sent"
    val outputSpeech = AlexaOutputSpeech("PlainText", responseQuote)
    val card = AlexaCard("Simple", "ECard", responseQuote)
    val reprompt = AlexaReprompt(outputSpeech)
    val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
    val resp = Json.toJson(AlexaResponse("1.0", Map(), alexaResponseType, true))
    logger.info(resp.toString)
    Future(Ok(resp))
  }

  def clearCache(): Unit = {
    cache.remove("1")
    cache.remove("2")
    cache.remove("3")
  }
}
