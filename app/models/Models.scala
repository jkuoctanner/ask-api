package models

import play.api.libs.json.Json

case class SystemUser(systemUserId: Long, customerId: Long, employeeId: String)

object SystemUser {
  implicit val systemUser = Json.format[SystemUser]
}

case class Person(firstName: String, lastName: String, department: String)
