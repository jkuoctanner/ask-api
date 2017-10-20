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
case class AlexaIntent(name: String, slots: Map[String, AlexaSlot])
case class AlexaRequestType(`type`: String, intent: Option[AlexaIntent])
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

object AlexaResponse {
  implicit val alexaOutputSpeech = Json.format[AlexaOutputSpeech]
  implicit val alexaCard = Json.format[AlexaCard]
  implicit val alexaReprompt = Json.format[AlexaReprompt]
  implicit val alexaResponseType = Json.format[AlexaResponseType]
  implicit val alexaResponse = Json.format[AlexaResponse]
}

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