package it.ldsoftware.webfleet.driver.flows.consumers

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.amqp.RabbitMqChannel
import it.ldsoftware.webfleet.commons.flows.EventConsumer
import it.ldsoftware.webfleet.driver.actors.Content.Event

import scala.concurrent.{ExecutionContext, Future}

class AmqpEventConsumer(amqp: RabbitMqChannel, destination: String)(implicit ec: ExecutionContext)
    extends EventConsumer[Event]
    with LazyLogging {

  override def consume(actorId: String, event: Event): Future[Done] =
    Future {
      amqp.publish(destination, actorId, event)
    }.map(_ => Done)

}
