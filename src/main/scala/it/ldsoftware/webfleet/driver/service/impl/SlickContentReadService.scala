package it.ldsoftware.webfleet.driver.service.impl

import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.read.dbio.Contents
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.service.ContentReadService
import it.ldsoftware.webfleet.driver.service.model._

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
class SlickContentReadService(db: Database)(implicit ec: ExecutionContext)
    extends ContentReadService
    with LazyLogging {

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

  override def search(filter: ContentFilter): Future[ServiceResult[List[ContentRM]]] = {
    val query = contents
      .filterOpt(filter.path)((c, path) => c.path === path)
      .filterOpt(filter.parent)((c, parent) => c.parent === parent)
      .filterOpt(filter.title)((c, title) => c.title.toLowerCase.like(s"%${title.toLowerCase}%"))
      .result

    db.run(query)
      .map { seq =>
        logger.debug(s"""${query.statements.mkString(" ")} found ${seq.size} results""")
        success(seq.toList)
      }
  }
}
// $COVERAGE-ON$
