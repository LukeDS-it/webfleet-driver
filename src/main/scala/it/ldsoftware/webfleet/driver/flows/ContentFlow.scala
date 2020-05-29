package it.ldsoftware.webfleet.driver.flows

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{EventEnvelope, Offset, Sequence}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, SharedKillSwitch}
import akka.{Done, NotUsed}
import it.ldsoftware.webfleet.driver.actors.Content
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class ContentFlow(readJournal: JdbcReadJournal, db: Database, consumers: Seq[ContentEventConsumer])(
    implicit ec: ExecutionContext,
    mat: Materializer
) {

  val Tag = "content"

  val contentSource: Source[EventEnvelope, Future[NotUsed]] =
    Source.futureSource {
      getLastOffset.map(readJournal.eventsByTag(Tag, _))
    }

  val processEvent: Flow[EventEnvelope, Offset, NotUsed] =
    Flow[EventEnvelope].map { envelope =>
      envelope.event match {
        case x: Content.Event =>
          consumers.foreach(_.consume(x))
          envelope.offset
        case e => throw new IllegalArgumentException(s"Cannot process $e")
      }
    }

  val saveOffset: Sink[Offset, Future[Done]] =
    Flow[Offset]
      .map(writeOffsetSql)
      .map(db.run(_))
      .toMat(Sink.ignore)(Keep.right)

  def run(killSwitch: SharedKillSwitch): Unit =
    contentSource
      .via(killSwitch.flow)
      .via(processEvent)
      .runWith(saveOffset)

  def getLastOffset: Future[Long] =
    db.run {
        sql"select last_offset from offset_store where tag = $Tag"
          .as[Long]
          .headOption
      }
      .map(_.getOrElse(0L))

  def writeOffsetSql(offset: Offset): DBIO[Int] =
    offset match {
      case Sequence(value) =>
          sqlu"""
        insert into offset_store(tag, last_offset)
         values ($Tag, $value)
          on conflict (tag) do
           update set last_offset = $value
        """
      case _ => throw new IllegalArgumentException(s"unexpected offset $offset")
    }

}

trait ContentEventConsumer {
  def consume(event: Content.Event): Unit
}
