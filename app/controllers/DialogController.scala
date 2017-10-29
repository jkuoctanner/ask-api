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
  val FIRST_NAME_SLOT = "firstName"
  val LAST_NAME_SLOT = "lastName"
  val PERSON_NAME_SLOT = "personName"

  //Enter a first name
  def alpha = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    val alexaRequest = request.body.validate[AlexaRequest].get
    alexaRequest.request.`type` match {
      case "IntentRequest" =>
        alexaRequest.request.dialogState match {
          case Some("STARTED") =>
            handleFirstNameUtterance(alexaRequest)
          case Some("IN_PROGRESS") =>
            logger.error("Should not be IN_PROGRESS")
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

  def handleFirstNameUtterance(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.intent match {
      case Some(intent) =>
        val firstName = intent.slots.get(FIRST_NAME_SLOT).get.value.get

        val directive = AlexaDirectiveSlot("firstName", Some("NONE"), Some(firstName))
        val slots = Map() + ("firstName" -> AlexaDirectiveSlot("firstName", Some("NONE"), Some(firstName)))
        val updatedIntent = AlexaUpdatedIntent("AlphaIntent", "NONE", slots)
        val directives = Seq(AlexaDirective("Dialog.Delegate", Some(updatedIntent)))
        val resp = Json.toJson(AlexaDirectiveResponse("1.0", false, directives))
        logger.info(resp.toString)
        Future(Ok(resp))
      case None =>
        logger.error("No intent for first name utterance")
        quitOnError()
    }
  }

  def handleInProgressUtterance(alexaRequest: AlexaRequest): Future[Result] = {
    val resp = Json.toJson(AlexaDirectiveResponse("1.0", false, Seq(AlexaDirective("Dialog.Delegate", None))))
    logger.info(resp.toString)
    Future(Ok(resp))
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
}
