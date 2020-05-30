package it.ldsoftware.webfleet.driver.service.impl

import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.read.dbio.Contents
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.service.ContentReadService

import scala.concurrent.Future

// $COVERAGE-OFF$
class SlickContentReadService(db: Database) extends ContentReadService {

  val contents = TableQuery[Contents]

  def insertContent(content: ContentRM): Future[ContentRM] =
    db.run(contents.returning(contents) += content)

  override def editContent(id: String, title: Option[String], desc: Option[String]): Future[Int] = {
    val action = (title, desc) match {
      case (Some(t), Some(d)) =>
        sqlu"update contents set title = $t, description = $d where path = $id"
      case (Some(t), None) =>
        sqlu"update contents set title = $t where path = $id"
      case (None, Some(d)) =>
        sqlu"update contents set description = $d where path = $id"
    }
    db.run(action)
  }

  override def deleteContent(id: String): Future[Int] =
    db.run(contents.filter(_.path === id).delete)
}
// $COVERAGE-ON$
