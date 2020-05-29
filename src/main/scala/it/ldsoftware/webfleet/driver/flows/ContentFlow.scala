package it.ldsoftware.webfleet.driver.flows

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import it.ldsoftware.webfleet.driver.actors.Content
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class ContentFlow(readJournal: JdbcReadJournal, db: Database, consumers: Seq[ContentEventConsumer])(
    implicit ec: ExecutionContext
) {

  val Tag = "content"

  def getLastOffset: Future[Long] =
    db.run {
        sql"select last_offset from offset_store where tag = $Tag"
          .as[Long]
          .headOption
      }
      .map(_.getOrElse(0L))

  def writeLastOffset(offset: Long): Future[Int] =
    db.run {
      sqlu"update offset_store set last_offset = $offset where tag = $Tag"
    }

  val input: Source[EventEnvelope, Future[NotUsed]] =
    Source.futureSource {
      getLastOffset.map { o => readJournal.eventsByTag(Tag, o) }
    }

  val consume: Sink[EventEnvelope, Future[Done]] =
    Sink.foreach[EventEnvelope] { env =>
      env.event match {
        case x: Content.Event => consumers.foreach(_.consume(x))
      }
    }

}

trait ContentEventConsumer {
  def consume(event: Content.Event): Unit
}
