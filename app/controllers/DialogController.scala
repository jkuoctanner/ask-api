package controllers

import models.http._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Result }
import play.mvc.Http.Response

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DialogController extends Controller with RequestProcessor {
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
    handleInProgressUtterance(alexaRequest)
  }

  def handleInProgressUtterance(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        if (intent.slots.get(DEPARTMENT_SLOT).isEmpty) {
          val personName = intent.slots.get(PERSON_NAME_SLOT).get.value.get

          val dName = AlexaDirectiveSlot("personName", Some("NONE"), Some(personName))
          val dept = AlexaDirectiveSlot("department", Some("NONE"), None)
          val slots = Map() + ("personName" -> dName) + ("department" -> dept)
          val updatedIntent = AlexaUpdatedIntent(INTENT_NAME, "NONE", slots)
          val directives = Seq(AlexaDirective("Dialog.ElicitSlot", Some(updatedIntent)))
          val outputSpeech = AlexaOutputSpeech("PlainText", "Is the person from Human Resources, or InfoTech or Labs?")
          val respType = AlexaDirectiveResponseType(false, Some(outputSpeech), directives)
          val resp = Json.toJson(AlexaDirectiveResponse("1.0", Map(), respType))
          logger.info(resp.toString)
          Future(Ok(resp))
        } else if (intent.confirmationStatus.isEmpty || intent.confirmationStatus.get.equals("NONE")) {
          val personName = intent.slots.get(PERSON_NAME_SLOT).get.value.get
          val deptVal = intent.slots.get(DEPARTMENT_SLOT).get.value.get

          val dName = AlexaDirectiveSlot("personName", Some("NONE"), Some(personName))
          val dept = AlexaDirectiveSlot("department", Some("NONE"), Some(deptVal))
          val slots = Map() + ("personName" -> dName) + ("department" -> dept)
          val updatedIntent = AlexaUpdatedIntent(INTENT_NAME, "NONE", slots)
          val directives = Seq(AlexaDirective("Dialog.ConfirmIntent", Some(updatedIntent)))
          val outputSpeech = AlexaOutputSpeech("PlainText", "I will send an eCard to " + personName + " from " + deptVal + ". Is that OK?")
          val respType = AlexaDirectiveResponseType(false, Some(outputSpeech), directives)
          val resp = Json.toJson(AlexaDirectiveResponse("1.0", Map(), respType))
          logger.info(resp.toString)
          Future(Ok(resp))
        } else if (intent.confirmationStatus.isDefined && intent.confirmationStatus.get.equals("CONFIRMED")) {
          handleCompleted(alexaRequest)
        } else {
          quitOnError()
        }
      case None =>
        logger.error("No intent for person name utterance")
        quitOnError()
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
        quitOnError()
    }
  }

  def quitOnError(): Future[Result] = {
    val responseQuote = "Error so No ECard sent"
    val outputSpeech = AlexaOutputSpeech("PlainText", responseQuote)
    val card = AlexaCard("Simple", "ECard", responseQuote)
    val reprompt = AlexaReprompt(outputSpeech)
    val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
    val resp = Json.toJson(AlexaResponse("1.0", Map(), alexaResponseType, true))
    logger.info(resp.toString)
    Future(Ok(resp))
  }
}
