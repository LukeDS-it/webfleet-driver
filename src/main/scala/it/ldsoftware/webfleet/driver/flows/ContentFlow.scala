package it.ldsoftware.webfleet.driver.flows

import akka.NotUsed
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{Offset, Sequence}
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{Materializer, SharedKillSwitch}
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.flows.ContentFlow._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ContentFlow(readJournal: JdbcReadJournal, db: Database, consumers: Seq[ContentEventConsumer])(
    implicit ec: ExecutionContext,
    mat: Materializer
) {

  def run(killSwitch: SharedKillSwitch): Unit =
    RestartSource
      .withBackoff(500.millis, maxBackoff = 20.seconds, randomFactor = 0.1) { () =>
        Source.futureSource {
          getLastOffset.map { offset =>
            processEvents(offset)
              .mapAsync(1)(writeOffset)
          }
        }
      }
      .via(killSwitch.flow)
      .runWith(Sink.ignore)

  def processEvents(offset: Long): Source[Offset, NotUsed] =
    readJournal.eventsByTag(Tag, offset).mapAsync(1) { envelope =>
      envelope.event match {
        case e: Content.Event =>
          Future
            .sequence(
              consumers.map(_.consume(envelope.persistenceId, e).recover(th => println(th)))
            )
            .map(_ => envelope.offset)
        case unknown => Future.failed(new IllegalArgumentException(s"Cannot process $unknown"))
      }
    }

  def writeOffset(offset: Offset): Future[Int] = db.run(writeOffsetSql(offset))

  def getLastOffset: Future[Long] = db.run(GetOffset).map(_.getOrElse(0L))

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

object ContentFlow {
  val Tag = "content"

  val GetOffset = sql"select last_offset from offset_store where tag = $Tag"
    .as[Long]
    .headOption
}
