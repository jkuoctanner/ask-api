package services

import javax.inject.Inject

import daos.SystemUserDAO
import models.SystemUser
import play.api.db.Database
import play.db.NamedDatabase
import utils.ResourceNotFoundException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SystemUserService @Inject() (@NamedDatabase("victories") db: Database,
                                   dao:                            SystemUserDAO,
                                  ) {

  def getCustomerId(systemUserId: String): Future[Long] = {
    Future {
      db.withTransaction { implicit conn =>
        val systemUser = dao.get(systemUserId)
        if(systemUser.isEmpty) throw ResourceNotFoundException("Cannot Find system User for sender")
        systemUser.get.customerId
      }
    }
  }

  def getPossibleRecipients(firstName: String, lastName: String, customerId: String): Future[Seq[SystemUser]] = {
    Future {
      db.withTransaction { implicit conn =>
        dao.get(firstName, lastName, customerId)
      }
    }
  }
}
