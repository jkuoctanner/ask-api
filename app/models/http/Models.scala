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
