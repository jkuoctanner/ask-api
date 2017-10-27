package daos

import anorm._
import java.sql.Connection
import models.SystemUser

class SystemUserDAO {
  def get(firstName: String, lastName: String, customerID: String)(implicit connection: Connection): Seq[SystemUser] = {
    SQL"""
        select su.system_user_id, su.customer_id, su.employee_id
        from system_user su
        where
        su.customer_id = $customerID and
        lower(su.first_name) = lower($firstName) and lower(su.last_name) = lower($lastName)
      """
      .executeQuery()
      .as(SystemUserDAO.systemUserParser.*)
  }

  def get(systemUserId: String)(implicit connection: Connection): Option[SystemUser] = {
    SQL"""
        select su.system_user_id, su.customer_id, su.employee_id
        from system_user su
        where su.system_user_id = $systemUserId
      """
      .executeQuery()
      .as(SystemUserDAO.systemUserParser.singleOpt)
  }
}

object SystemUserDAO {
  val systemUserParser = Macro.parser[SystemUser]("system_user_id", "customer_id", "employee_id")
}