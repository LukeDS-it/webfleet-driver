package it.ldsoftware.webfleet.driver.service.impl

import it.ldsoftware.webfleet.driver.service.HealthService
import it.ldsoftware.webfleet.driver.service.model.ApplicationHealth
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class BasicHealthService(db: Database)(implicit ec: ExecutionContext) extends HealthService {

  override def checkHealth: Future[ApplicationHealth] = checkDBHealth.map {
    case (str, bool) => ApplicationHealth(str, bool)
  }

  private def checkDBHealth: Future[(String, Boolean)] =
    db.run(sql"select 1".as[Int])
      .map(_ => ("ok", true))
      .recover(th => (th.getMessage, false))

}
