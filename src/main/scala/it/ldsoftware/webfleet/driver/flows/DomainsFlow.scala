package it.ldsoftware.webfleet.driver.flows

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, Folder, Published}
import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.util.RabbitMQUtils

import scala.concurrent.{ExecutionContext, Future}

class DomainsFlow(domainDestination: String, amqp: RabbitMQUtils)(
    implicit ec: ExecutionContext,
    timeout: Timeout,
    system: ActorSystem[_]
) extends LazyLogging {

  val sharding: ClusterSharding = ClusterSharding(system)
  val consumerQueueName = "webfleet-driver-consume-domains"

  amqp.createNamedQueueFor(domainDestination, consumerQueueName)

  amqp
    .getConsumerFor[DomainsFlow.Event](consumerQueueName)
    .consume {
      case Left(value)  => processError(value)
      case Right(value) => processEvent(value.entityId, value.content)
    }

  def processEvent(id: String, event: DomainsFlow.Event): Future[akka.Done] = event match {
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
        .map(_ => akka.Done)

    case DomainsFlow.Deleted(user) =>
      logger.info(s"The external event $event is controlling page deletion")
      sharding
        .entityRefFor(Key, s"$id/")
        .ask[Response](Delete(user, _))
        .map(_ => akka.Done)
  }

  def processError(error: Error): Future[akka.Done] = Future {
    logger.error(s"An error has occurred while parsing the events", error)
    akka.Done
  }

}

object DomainsFlow {
  sealed trait Event extends CborSerializable
  case class Created(form: DomainCreateForm, user: User) extends Event
  case class Deleted(user: User) extends Event

  case class DomainCreateForm(title: String, id: String, icon: String) extends CborSerializable

  case class FlowData[T](domainId: String, content: T)
}
