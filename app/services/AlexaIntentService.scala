package services

import java.util
import javax.inject.Inject

import com.google.inject.name.Named
import com.octanner.auth.{ OCTannerAuth, OCTannerAuthData, Scope, SmD }
import models.http.VictoriesEProductPayload
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.{ ExternalServiceException, ResourceNotFoundException }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AlexaIntentService @Inject() (
    ocTannerAuth:                                 OCTannerAuth,
    wsClient:                                     WSClient,
    @Named("VictoriesBaseApiUrl") val baseApiUrl: String) {

  val logger = Logger(this.getClass)

  val eCardData = VictoriesEProductPayload(false, true, 8011190, 13987799,
    "Test eCard to Bryan", false, 4509251, "2961723", "ECard", 2961698)

  val payload = Json.toJson[VictoriesEProductPayload](eCardData).toString()

  def handleIntent(systemUserId: String, customerId: String, intents: String): Future[String] = {
    val wsRequest = wsClient.url(baseApiUrl + "/give/submitEProduct")
      .withHeaders(("Authorization", getToken(systemUserId, customerId)), ("Content-Type", "application/json"))
    for {
      response <- wsRequest.post(payload)
    } yield {
      response.status match {
        case Status.OK => response.body
        case _ => {
          logger.info(s"Failed Service Call : ${baseApiUrl}/give/submitEProduct ${response.status}: ${response.body}")
          throw ExternalServiceException("A service call failed")
        }
      }
    }
  }

  def getToken(systemUserId: String, customerId: String): String = {
    "Bearer " + ocTannerAuth.encodeToken(new OCTannerAuthData()
      .setClientId("ask_service")
      .setScopes(util.EnumSet.allOf(classOf[Scope]))
      .setExpires(SmD.from(System.currentTimeMillis() + 1000l * 60l * 60l * 24))
      .setUserId(systemUserId) //system user id 2961723
      .setCustomerId(customerId)) //customerId 6971300
  }
}
