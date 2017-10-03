import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "HealthController" should {

    "get health check info" in {
      val health = route(app, FakeRequest(GET, "/octhc")).get

      status(health) mustBe OK
      contentType(health) mustBe Some("text/plain")
      contentAsString(health) must include("overall_status=good")
    }

  }

}
