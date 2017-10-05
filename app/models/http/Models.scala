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

case class AlexaIntentRequest(intents: String) extends HttpRequest

object AlexaIntentRequest {
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