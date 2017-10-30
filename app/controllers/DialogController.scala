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

  //Enter first and last names

  //Enter a message, first and last name

  //Enter a message, first and last name, and respond with a single choice of name and workplace. Select one.

  //Enter a message, first and last name, and respond with 2 choices of name and workplace. Select one.

  def handleStarted(alexaRequest: AlexaRequest): Future[Result] = {
    val respType = AlexaDirectiveResponseType(false, Seq(AlexaDirective("Dialog.Delegate", None)))
    val resp = Json.toJson(AlexaDirectiveResponse("1.0", Map(), respType))
    logger.info(resp.toString)
    Future(Ok(resp))
  }

  def handleInProgressUtterance(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        val personName = intent.slots.get(PERSON_NAME_SLOT).get.value.get

        val dF = AlexaDirectiveSlot("personName", Some("NONE"), Some(personName))
        val slots = Map() + ("personName" -> dF)
        val updatedIntent = AlexaUpdatedIntent(INTENT_NAME, "NONE", slots)
        val directives = Seq(AlexaDirective("Dialog.Delegate", Some(updatedIntent)))
        val respType = AlexaDirectiveResponseType(false, directives)
        val resp = Json.toJson(AlexaDirectiveResponse("1.0", Map(), respType))
        logger.info(resp.toString)
        Future(Ok(resp))
      case None =>
        logger.error("No intent for person name utterance")
        quitOnError()
    }
  }

  def handleCompleted(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        val firstName = intent.slots.get(FIRST_NAME_SLOT).get.value.get

        val responseQuote = "ECard sent to " + firstName
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

  def handleFirstLastNameUtterance(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        val firstName = intent.slots.get(FIRST_NAME_SLOT).get.value.get
        val lastName = intent.slots.get(LAST_NAME_SLOT).get.value.get

        val dF = AlexaDirectiveSlot("firstName", Some("NONE"), Some(firstName))
        val dL = AlexaDirectiveSlot("lastName", Some("NONE"), Some(lastName))
        val slots = Map() + ("firstName" -> dF) + ("lastName" -> dL)
        val updatedIntent = AlexaUpdatedIntent(INTENT_NAME, "NONE", slots)
        val directives = Seq(AlexaDirective("Dialog.Delegate", Some(updatedIntent)))
        val respType = AlexaDirectiveResponseType(false, directives)
        val resp = Json.toJson(AlexaDirectiveResponse("1.0", Map(), respType))
        logger.info(resp.toString)
        Future(Ok(resp))
      case None =>
        logger.error("No intent for first last names utterance")
        quitOnError()
    }
  }
}
