package controllers

import javax.inject._

import models.http._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.{ EcardService, EmployeeSearchService }
import utils.BadRequestException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AlexaRequestController @Inject() (service: EcardService, searchSvc: EmployeeSearchService) extends Controller with RequestProcessor {
  val logger = Logger(this.getClass)
  val FIRST_NAME_SLOT = "firstName"
  val LAST_NAME_SLOT = "lastName"
  val RECIPIENT_SLOT = "Recipient"

  def handleRequest = Action.async(parse.json) { request =>
    val jsResult = request.body.validate[AlexaRequest]
    processRequest[AlexaRequest](jsResult, createAlexaIntentAction("2961698"))
  }

  def testRequest(name: String) = Action.async { request =>
    searchSvc.searchPossibleEmployees(name).map { result =>
      Ok(Json.toJson(result))
    }
  }

  def testRequest2 = Action.async(parse.json) { request =>
    logger.info(request.body.toString())
    Future(Ok(Json.parse("""{"version":"1.0","sessionAttributes":{"supportedHoriscopePeriods":{"daily":true,"weekly":false,"monthly":false}},"response":{"outputSpeech":{"type":"PlainText","text":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless. Can I help you with anything else?"},"card":{"type":"Simple","title":"Horoscope","content":"Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."},"reprompt":{"outputSpeech":{"type":"PlainText","text":"Can I help you with anything else?"}},"shouldEndSession":true}}""")))
  }

  private def createAlexaIntentAction(senderSystemUserId: String)(alexaRequest: AlexaRequest): Future[Result] = {
    alexaRequest.request.`type` match {
      case "IntentRequest" => {
        logger.info("IntentRequest ")
        alexaRequest.request.intent match {
          case Some(intent) =>
            if (intent.name.equalsIgnoreCase("GivaAnECard")) {
              val fullName = intent.slots.get(RECIPIENT_SLOT).getOrElse(throw BadRequestException("Recipient Slot is not populated")).value.get
              for {
                possibleRecipients <- searchSvc.searchPossibleEmployees(fullName)
                if possibleRecipients.size > 0
                recipient = possibleRecipients.head
                answer <- service.sendEcard(senderSystemUserId, recipient.systemUserId.toString)
              } yield {
                logger.info("response = " + answer)
                val responseQuote = "ECard sent to " + recipient.firstName + " " + recipient.lastName + " from " + recipient.businessUnit
                val outputSpeech = AlexaOutputSpeech("PlainText", responseQuote)
                val card = AlexaCard("Simple", "ECard", responseQuote)
                val reprompt = AlexaReprompt(outputSpeech)
                val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
                val resp = Json.toJson(AlexaResponse("1.0", Map(), alexaResponseType, true))
                logger.info(resp.toString)
                Ok(resp)
              }
            } else {
              logger.error("Unknown intent called " + intent.name)
              Future(BadRequest)
            }
          case _ =>
            logger.error("No intent for an IntentRequest")
            Future(BadRequest)
        }
      }
      case _ =>
        logger.error("No intent for an IntentRequest")
        Future(BadRequest)
    }
  }
}