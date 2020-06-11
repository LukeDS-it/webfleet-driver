package it.ldsoftware.webfleet.driver.flows.consumers

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.syntax._
import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.flows.ContentEventConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.concurrent.{ExecutionContext, Future}

class KafkaEventConsumer(kafkaProducer: KafkaProducer[String, String], topic: String)(
    implicit ec: ExecutionContext
) extends ContentEventConsumer
    with LazyLogging {

  override def consume(actorId: String, event: Event): Future[Done] = event match {
    case Created(_, _, _) | Updated(_, _, _) | Deleted(_, _) =>
      logger.debug(s"Sending ${event.getClass.getSimpleName} event to $topic")
      Future(event.asJson.noSpaces)
        .map(new ProducerRecord[String, String](topic, _))
        .map(kafkaProducer.send)
        .map(_.get)
        .recover(th => logger.error(s"Error while sending $event to $topic", th))
        .map(_ => Done)

    case _ =>
      logger.debug(s"Skipping ${event.getClass.getSimpleName}")
      Future.successful(Done)
  }

}
