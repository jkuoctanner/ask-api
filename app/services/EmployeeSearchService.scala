package services

import javax.inject.Inject

import com.google.inject.name.Named
import models.{ Employee, SolrResponseDoc }
import play.api.Logger
import play.api.http.Status
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmployeeSearchService @Inject() (ws: WSClient, @Named("SolrSearchUrl") val searchURL: String) {
  val logger = Logger(this.getClass)

  def searchPossibleEmployees(name: String): Future[Seq[Employee]] = {
    val split = name.split(" ")
    if (split.size < 1)
      Future.successful(Seq())
    else if (split.size == 1)
      searchPossibleEmployees(split.head, None)
    else
      searchPossibleEmployees(split.head, Some(split.last))
  }

  def searchPossibleEmployees(firstName: String, lastName: Option[String]): Future[Seq[Employee]] = {
    val url = if (lastName.isDefined)
      s"$searchURL?q=first_name_sound:$firstName%20AND%20last_name_sound:${lastName.get}"
    else
      s"$searchURL?q=first_name_sound:$firstName"

    ws.url(url)
      .withHttpHeaders(("Accept", "application/json"))
      .get()
      .map { response =>
        response.status match {
          case Status.OK =>
            val docs = (response.json \ "response" \ "docs").as[Seq[SolrResponseDoc]]
            val searchResults = docs.map { d => Employee(d.first_name.head, d.last_name.head, d.business_unit.head, d.id.toLong, d.employee_id) }
            logger.info(s"$searchResults")
            searchResults
          case _ =>
            logger.error(s"Failed Solr service call $url: ${response.status}: ${response.body}")
            Seq()
        }
      }
  }
}
