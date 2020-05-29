package it.ldsoftware.webfleet.driver.flows

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{EventEnvelope, Offset}
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

  def writeLastOffset(offset: Offset): Future[Int] =
    db.run {
      sqlu"update offset_store set last_offset = 0 where tag = $Tag"
    }

  val input: Source[EventEnvelope, Future[NotUsed]] =
    Source.futureSource {
      getLastOffset.map { o => readJournal.eventsByTag(Tag, o) }
    }

  val consume: Sink[EventEnvelope, Future[Done]] =
    Sink.foreach[EventEnvelope] { env =>
      env.event match {
        case x: Content.Event =>
          consumers.foreach(_.consume(x))
          writeLastOffset(env.offset)
      }
    }

}

trait ContentEventConsumer {
  def consume(event: Content.Event): Unit
}
