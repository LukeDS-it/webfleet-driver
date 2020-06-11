package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.flows.{ContentEventConsumer, ContentFlow, OffsetManager}
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

  implicit val ec: ExecutionContext = testKit.system.executionContext

  "The event processor" should {
    "process events saved from Content" in {
      val readJournal = mock[JdbcReadJournal]
      val db = mock[OffsetManager]
      val envelope = makeEnvelope

      val probe = testKit.createTestProbe[String]("waiting")

      val flow = new ContentFlow(readJournal, db, new ProbeEventConsumer(probe))

      when(db.getLastOffset("ProbeEventConsumer")).thenReturn(Future.successful(0L))
      when(readJournal.eventsByTag("content", 0)).thenReturn(Source(Seq(envelope)))

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

  class ProbeEventConsumer(probe: TestProbe[String]) extends ContentEventConsumer {
    override def consume(str: String, evt: Event): Future[Done] =
      Future(probe.ref ! s"$str: ${evt.getClass.getSimpleName}")
      .map(_ => akka.Done)
  }
}
