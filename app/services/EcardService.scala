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
import utils.ExternalServiceException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EcardService @Inject() (
    ocTannerAuth:                                 OCTannerAuth,
    ws:                                           WSClient,
    @Named("VictoriesBaseApiUrl") val baseApiUrl: String) {

  val logger = Logger(this.getClass)
  val CUSTOMER_ID = "6971300"
  val CC_MANAGER = false
  val COPY_SENDER = true
  val CORPORATE_VALUE_ID = 8011190
  val EPRODUCT_ID = 13987799
  val ECARD_MESSAGE = "Ecard from Alexa"
  val NOTIFY_VIEWED = false
  val PROGRAM_ID = 4509251

  def sendEcard(senderSystemUserId: String, recipientSystemUserId: String): Future[String] = {
    val url = (baseApiUrl + "/give/submitEProduct")
    val eCardData = VictoriesEProductPayload(CC_MANAGER, COPY_SENDER, CORPORATE_VALUE_ID,
      EPRODUCT_ID, ECARD_MESSAGE, NOTIFY_VIEWED, PROGRAM_ID,
      recipientSystemUserId, "ECard", senderSystemUserId.toLong)
    val token = getToken(senderSystemUserId, CUSTOMER_ID)

    ws.url(url)
      .withHeaders(("Authorization", token), ("Content-Type", "application/json"))
      .post(Json.toJson[VictoriesEProductPayload](eCardData).toString)
      .map { response =>
        response.status match {
          case Status.OK =>
            response.body
          case _ =>
            logger.info(s"Failed Service Call : $url ${response.status}: ${response.body}")
            throw ExternalServiceException("A service call failed")
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
