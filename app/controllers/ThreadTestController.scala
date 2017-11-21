package controllers

import play.api.mvc.{ InjectedController }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ThreadTestController extends InjectedController {
  def blockingThread(count: Int, time: Int) = Action.async { request =>
    for {
      x <- Future.sequence(getFutures(count, time))
    } yield {
      Ok(s"Futures created=${count}, delay=${time} mSec, Futures completed=${x.length.toString}")
    }
  }

  def getFutures(count: Int, time: Int): Seq[Future[Int]] = {
    var s: Seq[Future[Int]] = Seq()
    for (i <- 1 to count) {
      s = s :+ Future {
        Thread.sleep(time)
        i
      }
    }
    s
  }
}
