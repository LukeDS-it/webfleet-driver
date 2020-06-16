package it.ldsoftware.webfleet.driver.flows

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.parser.decode
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, Folder, Published}
import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable
import it.ldsoftware.webfleet.driver.flows.DomainsFlow.FlowData
import it.ldsoftware.webfleet.driver.security.User

import scala.concurrent.{ExecutionContext, Future}

class DomainsFlow(domainTopic: String, consumerConfig: ConsumerSettings[String, String])(
    implicit ec: ExecutionContext,
    timeout: Timeout,
    system: ActorSystem[_]
) extends LazyLogging {

  val sharding: ClusterSharding = ClusterSharding(system)

  Consumer
    .sourceWithOffsetContext(consumerConfig, Subscriptions.topics(domainTopic))
    .map(record => FlowData(record.key(), record.value()))
    .map(data => (data.domainId, decode[DomainsFlow.Event](data.content)))
    .map {
      case (id, Right(event)) => processEvent(id, event)
      case (id, Left(error)) => processError(id, error)
    }
    .map(_ => NotUsed)
    .runWith(Committer.sinkWithOffsetContext(CommitterSettings(system.classicSystem)))

  def processEvent(id: String, event: DomainsFlow.Event): Future[Response] = event match {
    case DomainsFlow.Created(form, user) =>
      logger.info(s"The external event $event is controlling page creation")
      val cf = CreateForm(
        form.title,
        s"${form.id}/",
        Folder,
        "Root of your new website!",
        "Welcome to your new website!",
        contentStatus = Some(Published)
      )
      sharding
        .entityRefFor(Key, s"${form.id}/")
        .ask[Response](Create(cf, user, _))

    case DomainsFlow.Deleted(user) =>
      logger.info(s"The external event $event is controlling page deletion")
      sharding
        .entityRefFor(Key, s"$id/")
        .ask[Response](Delete(user, _))
  }

  def processError(id: String, error: io.circe.Error): Future[Response] = Future {
    logger.error(s"Could not parse record for key $id", error)
    Content.Done
  }

}

object DomainsFlow {
  sealed trait Event extends CborSerializable
  case class Created(form: DomainCreateForm, user: User) extends Event
  case class Deleted(user: User) extends Event

  case class DomainCreateForm(title: String, id: String, icon: String) extends CborSerializable

  case class FlowData[T](domainId: String, content: T)
}
