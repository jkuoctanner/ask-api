package models

import play.api.libs.json.Json

case class Employee(firstName: String, lastName: String, businessUnit: String, systemUserId: Long, employeeId: Long)

object Employee {
  implicit val employee = Json.format[Employee]
}

case class SolrResponseDoc(id: String, first_name: Seq[String], last_name: Seq[String], employee_id: Long, business_unit: Seq[String])

object SolrResponseDoc {
  implicit val doc = Json.format[SolrResponseDoc]
}
