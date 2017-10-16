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

case class AlexaSession(`new`: Boolean, sessionId: String, application: String, user: String)
case class AlexaSlot(name: String, value: String)
case class AlexaIntent(name: String, slots: Map[String, AlexaSlot])
case class AlexaRequest(`type`: String, intent: AlexaIntent)

case class AlexaIntentRequest(session: AlexaSession, request: AlexaRequest) extends HttpRequest

object AlexaIntentRequest {
  implicit val alexaSessionFormat = Json.format[AlexaSession]
  implicit val alexaSlot = Json.format[AlexaSlot]
  implicit val alexaIntent = Json.format[AlexaIntent]
  implicit val alexaRequestFormat = Json.format[AlexaRequest]
  implicit val alexaIntentRequest = Json.format[AlexaIntentRequest]
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