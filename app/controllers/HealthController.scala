package controllers

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class HealthController @Inject() extends Controller {
  def health = Action {
    Ok("overall_status=good")
  }
}
