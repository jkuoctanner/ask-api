import javax.inject.{ Inject, Singleton }

import models.http._
import models.http.HttpModels._
import play.api.Logger
import play.api.cache.SyncCacheApi
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import utils.ResourceNotFoundException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

/**
 * Top-level error handler for both client (HTTP status code 4xx) and server (HTTP status code 5xx) errors.
 *
 * This class should be the only class responsible for determining the JSON representation of errors.
 * That is, make the appropriate changes here to control the format of the JSON representation of errors.
 */
@Singleton
class ErrorHandler @Inject() (cache: SyncCacheApi) extends HttpErrorHandler {
  val logger = Logger(this.getClass)

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    logger.error("Client error:" + message)
    quit("No eCard sent. Bye.")
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    exception match {
      case _ =>
        val message = "A server error occurred: " + exception.getMessage
        logger.error(message, exception)
        quit("No eCard sent. Bye.")
    }
  }

  def quit(responseQuote: String): Future[Result] = {
    clearCache(4)
    val outputSpeech = AlexaOutputSpeech("PlainText", Some(responseQuote), None)
    val card = AlexaCard("Simple", "ECard", responseQuote)
    val reprompt = AlexaReprompt(outputSpeech)
    val alexaResponseType = AlexaResponseType(outputSpeech, card, reprompt)
    val resp = Json.toJson(AlexaResponse("1.0", Map(), alexaResponseType, true))
    logger.info(resp.toString)
    Future(Ok(resp))
  }

  def clearCache(size: Int): Unit = {
    for (i <- 1 to size) {
      cache.remove(String.valueOf(i))
    }
  }
}
