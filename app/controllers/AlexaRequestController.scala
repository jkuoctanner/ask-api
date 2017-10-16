package controllers

import javax.inject._

import models.http.AlexaIntentRequest
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import services.AlexaIntentService

@Singleton
class AlexaRequestController @Inject() (service: AlexaIntentService) extends Controller with RequestProcessor {
  val logger = Logger(this.getClass)

  def handleRequest = Action.async(parse.json) { request =>
    val jsResult = request.body.validate[AlexaIntentRequest]
    processRequest[AlexaIntentRequest](jsResult, createAlexaIntentAction("2961698", "1543530"))
  }

  def testRequest = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    Future(Ok(Json.toJson("{}")))
  }

  def testRequest2 = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
  }

  private def createAlexaIntentAction(userId: String, customerId: String)(request: AlexaIntentRequest): Future[Result] = {
    for {
      answer <- service.handleIntent(userId, customerId, "")
    } yield {
      logger.info("response = " + answer)
      Ok
    }
  }

  def brenDialogHandler = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    val alexaRequest = request.body.validate[AlexaIntentRequest].get
    alexaRequest.request.`type` match {
      case "LaunchRequest" =>
        logger.info("launchRequest")
        getGiveAnECardResponse()
      case "IntentRequest" =>
        logger.info("IntentRequest")
        Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
      case "SessionEndedRequest" =>
        logger.info("SessionEndedRequest")
        Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
    }
  }

  private def getGiveAnECardResponse(): Future[Result] = {
    val firstNamePrompt = "Say the first name"
    val speechOutput = "<speak>" + firstNamePrompt + "</speak>"
    //val repromptText = "I can lead you through providing a city and " + "day of the week to get tide information, " + "or you can simply open Tide Pooler and ask a question like, " + "get tide information for Seattle on Saturday. " + "For a list of supported cities, ask what cities are supported. " + whichCityPrompt
    //newAskResponse(speechOutput, true, repromptText, false)
    Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"test":"xxx"},"response":{"outputSpeech":{"type":"PlainText","text":"Say the first name"},"card":{"type":"Simple","title":"ECard","content":"Say the first name"},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Say the first name"}},"shouldEndSession":false}}""")))
  }
}