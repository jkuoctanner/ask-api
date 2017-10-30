package models.http

import play.api.libs.json.Json

object HttpModels {
  final case class Error(title: String, detail: String)
  final case class ErrorResponse(errors: Seq[Error])

  implicit val ErrorWrites = Json.writes[Error]
  implicit val ErrorResponseWrites = Json.writes[ErrorResponse]

  def createErrorResponse(title: String, message: String): ErrorResponse = ErrorResponse(Seq(Error(title, message)))
}

sealed trait HttpRequest

//For Alexa Request
case class AlexaSession(`new`: Boolean, sessionId: String, attributes: Map[String, String])
case class AlexaSlot(name: String, value: Option[String])
case class AlexaIntent(name: String, confirmationStatus: Option[String], slots: Map[String, AlexaSlot])
case class AlexaRequestType(`type`: String, intent: Option[AlexaIntent], dialogState: Option[String])
case class AlexaRequest(session: AlexaSession, request: AlexaRequestType) extends HttpRequest

object AlexaRequest {
  implicit val alexaSessionFormat = Json.format[AlexaSession]
  implicit val alexaSlot = Json.format[AlexaSlot]
  implicit val alexaIntent = Json.format[AlexaIntent]
  implicit val alexaRequestFormat = Json.format[AlexaRequestType]
  implicit val alexaIntentRequest = Json.format[AlexaRequest]
}

//For Alexa Response
case class AlexaOutputSpeech(`type`: String, text: String)
case class AlexaCard(`type`: String, title: String, content: String)
case class AlexaReprompt(outputSpeech: AlexaOutputSpeech)
case class AlexaResponseType(outputSpeech: AlexaOutputSpeech, card: AlexaCard, reprompt: AlexaReprompt)
case class AlexaResponse(version: String, sessionAttributes: Map[String, String], response: AlexaResponseType, shouldEndSession: Boolean)

case class AlexaDirectiveSlot(name: String, confirmationStatus: Option[String], value: Option[String])
case class AlexaUpdatedIntent(name: String, confirmationStatus: String, slots: Map[String, AlexaDirectiveSlot])
case class AlexaDirective(`type`: String, updatedIntent: Option[AlexaUpdatedIntent])
case class AlexaDirectiveResponseType(shouldEndSession: Boolean, outputSpeech: Option[AlexaOutputSpeech], directives: Seq[AlexaDirective])
case class AlexaDirectiveResponse(version: String, sessionAttributes: Map[String, String], response: AlexaDirectiveResponseType)

object AlexaResponse {
  implicit val alexaOutputSpeech = Json.format[AlexaOutputSpeech]
  implicit val alexaCard = Json.format[AlexaCard]
  implicit val alexaReprompt = Json.format[AlexaReprompt]
  implicit val alexaResponseType = Json.format[AlexaResponseType]
  implicit val alexaResponse = Json.format[AlexaResponse]
  implicit val alexaSlot = Json.format[AlexaSlot]
}
object AlexaDirectiveResponse {
  implicit val alexaOutputSpeech = Json.format[AlexaOutputSpeech]
  implicit val alexaDirectiveSlot = Json.format[AlexaDirectiveSlot]
  implicit val alexaUpdatedIntent = Json.format[AlexaUpdatedIntent]
  implicit val alexaDirective = Json.format[AlexaDirective]
  implicit val alexaDirectiveResponseType = Json.format[AlexaDirectiveResponseType]
  implicit val alexaDirectiveResponse = Json.format[AlexaDirectiveResponse]
}
//false,
//true,
//8011190,
//13987799,
//"Test eCard to Bryan",
//false,
//4509251,
//"2961723",
//"ECard",
//2961698 --> Ryan

//brendan => 7242784

//"2961698", "1543530"
case class VictoriesEProductPayload(
    ccManager:        Boolean,
    copySender:       Boolean,
    corporateValueId: Long,
    eProductId:       Long,
    message:          String,
    notifyViewed:     Boolean,
    programId:        Long,
    recipientIds:     String,
    recognitionType:  String,
    recognizerId:     Long)

object VictoriesEProductPayload {
  implicit val victoriesEProductPayload = Json.format[VictoriesEProductPayload]
}
