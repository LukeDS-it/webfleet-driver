package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.flows.ContentFlow._
import it.ldsoftware.webfleet.driver.flows.{ContentEventConsumer, ContentFlow}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}

class EventProcessorSpec
    extends CQRSActorSpec
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with MockitoSugar {

  import slick.jdbc.PostgresProfile.api._

  implicit val ec: ExecutionContext = testKit.system.executionContext

  "The event processor" should {
    "process events saved from Content" in {
      val readJournal = mock[JdbcReadJournal]
      val db = mock[Database]
      val envelope = makeEnvelope

      when(db.run(GetOffset)).thenReturn(Future.successful(Some(0L)))
      when(readJournal.eventsByTag(Tag, 0))
        .thenReturn(Source(Seq(envelope)))

      val probe = testKit.createTestProbe[String]("waiting")

      val callProbe: ContentEventConsumer = (str: String, evt: Event) =>
        probe.ref ! s"$str: ${evt.getClass.getSimpleName}"

      val flow = new ContentFlow(readJournal, db, Seq(callProbe))

      EventProcessor.init(system, flow)

      probe.expectMessage("/: Created")
    }
  }

  def makeEnvelope: EventEnvelope = EventEnvelope(
    akka.persistence.query.Sequence(1),
    "/",
    1L,
    Created(rootForm, superUser, ZonedDateTime.now()),
    ZonedDateTime.now.toInstant.getEpochSecond
  )
}
