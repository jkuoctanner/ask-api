package models

import play.api.libs.json.Json

case class SystemUser(systemUserId: Long, customerId: Long, employeeId: String) //TODO add first name, last name, department

object SystemUser {
  implicit val systemUser = Json.format[SystemUser]
}

case class Person(firstName: String, lastName: String, department: String, systemUserId: Long, customerId: Long, employeeId: String)
