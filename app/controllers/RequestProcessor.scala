package controllers

import models.http.HttpModels._
import models.http.HttpRequest
import play.api.Logger
import play.api.libs.json.{ JsPath, JsResult, Json, JsonValidationError }
import play.api.mvc.{ Controller, Result }

import scala.concurrent.Future

/**
 * Mix-in trait for controllers that need to process request body.
 */
trait RequestProcessor {
  this: Controller =>

  val logger: Logger

  def processRequest[A <: HttpRequest](result: JsResult[A], action: A => Future[Result]): Future[Result] = {
    result.fold(
      (errors: Seq[(JsPath, Seq[JsonValidationError])]) => {
        errors.foreach { e =>
          logger.error(s"field: ${e._1}, errors: ${e._2}")
        }
        Future.successful(BadRequest(Json.toJson(createErrorResponse("Bad Request", "Request body does not conform to API spec."))))
      },
      parsedRequest => action(parsedRequest)
    )
  }
}
