package services

import java.util
import javax.inject.Inject

import com.google.inject.name.Named
import com.octanner.auth.{ OCTannerAuth, OCTannerAuthData, Scope, SmD }
import models.SystemUser
import models.http.{ AlexaIntent, VictoriesEProductPayload }
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.{ BadRequestException, ExternalServiceException }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AlexaIntentService @Inject() (
    ocTannerAuth:                                 OCTannerAuth,
    wsClient:                                     WSClient,
    systemUserService:                            SystemUserService,
    @Named("VictoriesBaseApiUrl") val baseApiUrl: String) {

  val logger = Logger(this.getClass)

  def searchForRecipients(senderSystemUserId: String, recipientFirstName: String, recipientLastName: String): Future[Seq[SystemUser]] = {
    for {
      customerId <- systemUserService.getCustomerId(senderSystemUserId)
      possibleRecipients <- systemUserService.getPossibleRecipients(recipientFirstName, recipientLastName, customerId.toString)
    } yield possibleRecipients
  }

  def sendECard(senderSystemUserId: String, recipientSystemUserId: String, customerId: Long): Future[String] = {
    val wsRequest = wsClient.url(baseApiUrl + "/give/submitEProduct")
    val eCardData = VictoriesEProductPayload(false, true, 8011190, 13987799, "Test eCard to Bryan", false, 4509251, recipientSystemUserId, "ECard", senderSystemUserId.toLong)
    val token = getToken(senderSystemUserId, customerId.toString)
    logger.info(s"$baseApiUrl/give/submitEProduct $token")
    wsRequest.withHeaders(("Authorization", token), ("Content-Type", "application/json"))
      .post(Json.toJson[VictoriesEProductPayload](eCardData).toString())
      .map { response =>
        response.status match {
          case Status.OK =>
            response.body
          case _ => {
            logger.info(s"Failed Service Call : ${baseApiUrl}/give/submitEProduct ${response.status}: ${response.body}")
            throw ExternalServiceException("A service call failed")
          }
        }
      }
  }

  def handleIntent(senderSystemUserId: String, recipientFirstName: String, recipientLastName: String): Future[String] = {
    val wsRequest = wsClient.url(baseApiUrl + "/give/submitEProduct")
    val result = for {
      customerId <- systemUserService.getCustomerId(senderSystemUserId)
      possibleRecipients <- systemUserService.getPossibleRecipients(recipientFirstName, recipientLastName, customerId.toString)
    } yield {
      if (possibleRecipients.isEmpty) throw BadRequestException(s"Cannot Find Recipient for $recipientFirstName $recipientLastName")
      logger.info(s"possible Recipients found $possibleRecipients")

      val recipientSystemUserId = possibleRecipients.head.systemUserId.toString
      val eCardData = VictoriesEProductPayload(false, true, 8011190, 13987799, "Test eCard to Bryan", false, 4509251, recipientSystemUserId, "ECard", senderSystemUserId.toLong)
      val token = getToken(senderSystemUserId, customerId.toString)

      logger.info(s"$baseApiUrl/give/submitEProduct $token")
      wsRequest.withHeaders(("Authorization", token), ("Content-Type", "application/json"))
        .post(Json.toJson[VictoriesEProductPayload](eCardData).toString())
        .map { response =>
          response.status match {
            case Status.OK =>
              response.body
            case _ => {
              logger.info(s"Failed Service Call : ${baseApiUrl}/give/submitEProduct ${response.status}: ${response.body}")
              throw ExternalServiceException("A service call failed")
            }
          }
        }
    }
    result.flatten
  }

  def getToken(systemUserId: String, customerId: String): String = {
    "Bearer " + ocTannerAuth.encodeToken(new OCTannerAuthData()
      .setClientId("ask_service")
      .setScopes(util.EnumSet.allOf(classOf[Scope]))
      .setExpires(SmD.from(System.currentTimeMillis() + 1000l * 60l * 60l * 24))
      .setUserId(systemUserId) //system user id 2961723
      .setCustomerId(customerId)) //customerId 6971300
  }

  def extractRecipientFirstAndLastName(fullName: String): (String, String) = {
    logger.info(s"Full Name : $fullName")
    val split = fullName.split(" ")
    logger.info(s"First Name: ${split.head}; Last Name : ${split.last}")
    (split.head, split.last)
  }
}
