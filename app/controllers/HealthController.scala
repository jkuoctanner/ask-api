package controllers

import javax.inject._

import com.octanner.logging.PlatformLogger
import play.api.mvc._

@Singleton
class HealthController @Inject() extends Controller {

  val logger = PlatformLogger("HealthController")

  def health = Action {
    Ok("overall_status=good")
  }
}
