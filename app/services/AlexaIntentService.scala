package services

import javax.inject.Inject

import models._
import play.api.Logger
import utils.ResourceNotFoundException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AlexaIntentService @Inject() () {

  val logger = Logger(this.getClass)

  def handleIntent(userId: String, intents: String): Future[String] = {
    for {
      answer <- Future(userId + " " + intents)
    } yield answer
  }
}
