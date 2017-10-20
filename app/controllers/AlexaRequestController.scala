package controllers

import javax.inject._

import models.http._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import services.AlexaIntentService

@Singleton
class AlexaRequestController @Inject() (service: AlexaIntentService) extends Controller with RequestProcessor {
  val logger = Logger(this.getClass)
  val FIRST_NAME_SLOT = "firstName"
  val LAST_NAME_SLOT = "lastName"

  def handleRequest = Action.async(parse.json) { request =>
    val jsResult = request.body.validate[AlexaRequest]
    processRequest[AlexaRequest](jsResult, createAlexaIntentAction("2961698", "1543530"))
  }

  def testRequest = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    Future(Ok(Json.toJson("{}")))
  }

  def testRequest2 = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
  }

  private def createAlexaIntentAction(userId: String, customerId: String)(request: AlexaRequest): Future[Result] = {
    for {
      answer <- service.handleIntent(userId, customerId, "")
    } yield {
      logger.info("response = " + answer)
      Ok
    }
  }

  def brenDialogHandler = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    val alexaRequest = request.body.validate[AlexaRequest].get
    alexaRequest.request.`type` match {
      case "LaunchRequest" =>
        logger.info("launchRequest")
        getGiveAnECardResponse()
      case "IntentRequest" =>
        logger.info("IntentRequest ")
        alexaRequest.request.intent match {
          case Some(intent) =>
            if (intent.name.equalsIgnoreCase("GivaAnECard")) {
              val firstName = intent.slots.get(FIRST_NAME_SLOT)
              val lastName = intent.slots.get(LAST_NAME_SLOT)
              logger.info("firstName = " + firstName + " lastName = " + lastName)
              if (firstName.isDefined) {
                logger.info("handleFirstNameDialogRequest()")
                handleFirstNameDialogRequest(firstName.get.value.get, alexaRequest.session)
              } else {
                if (lastName.isDefined) {
                  logger.info("handleLastNameDialogRequest()")
                  handleLastNameDialogRequest(lastName.get.value.get, alexaRequest.session)
                } else {
                  logger.error("Last name was not defined")
                  Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
                }
              }
            } else if ("AMAZON.StopIntent".equals(intent.name)) {
              logger.info("intent name = " + intent.name)
              Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
            } else {
              logger.error("Unknown intent called " + intent.name)
              Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
            }
          case _ =>
            logger.error("No intent for an IntentRequest")
            Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
        }
      case "SessionEndedRequest" =>
        logger.info("SessionEndedRequest")
        Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
    }
  }

  def handleFirstNameDialogRequest(firstName: String, session: AlexaSession): Future[Result] = {
    val sessionAttributes = session.attributes + (FIRST_NAME_SLOT -> firstName)
    val outputSpeech = AlexaOutputSpeech("PlainText", "Say the last name")
    val card = AlexaCard("Simple", "ECard", "Say the first name")
    val reprompt = AlexaReprompt(outputSpeech)
    val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
    Future(Ok(Json.toJson(AlexaResponse("1.0", sessionAttributes, alexaResponseType, false))))
  }

  def handleLastNameDialogRequest(lastName: String, session: AlexaSession): Future[Result] = {
    val sessionAttributes = session.attributes + (LAST_NAME_SLOT -> lastName)
    val responseQuote = "ECard sent to " + session.attributes.get(FIRST_NAME_SLOT) + " " + lastName
    val outputSpeech = AlexaOutputSpeech("PlainText", responseQuote)
    val card = AlexaCard("Simple", "ECard", responseQuote)
    val reprompt = AlexaReprompt(outputSpeech)
    val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
    Future(Ok(Json.toJson(AlexaResponse("1.0", session.attributes, alexaResponseType, true))))
  }

  private def getGiveAnECardResponse(): Future[Result] = {
    val firstNamePrompt = "Say the first name"
    val speechOutput = "<speak>" + firstNamePrompt + "</speak>"
    //val repromptText = "I can lead you through providing a city and " + "day of the week to get tide information, " + "or you can simply open Tide Pooler and ask a question like, " + "get tide information for Seattle on Saturday. " + "For a list of supported cities, ask what cities are supported. " + whichCityPrompt
    //newAskResponse(speechOutput, true, repromptText, false)
    Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"test":"xxx"},"response":{"outputSpeech":{"type":"PlainText","text":"Say the first name"},"card":{"type":"Simple","title":"ECard","content":"Say the first name"},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Say the first name"}},"shouldEndSession":false}}""")))
  }
}