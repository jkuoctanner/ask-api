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

    processRequest[AlexaIntentRequest](jsResult, createAlexaIntentAction("2961698"))
  }

  private def createAlexaIntentAction(userId: String)(request: AlexaIntentRequest): Future[Result] = {
    for {
      answer <- service.handleIntent(userId, request.intents)
    } yield {
      Created(Json.toJson(answer))
    }
  }
}