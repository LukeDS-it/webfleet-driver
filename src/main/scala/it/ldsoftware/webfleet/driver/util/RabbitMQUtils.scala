package it.ldsoftware.webfleet.driver.util

import com.rabbitmq.client._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import scala.concurrent.ExecutionContext

class RabbitMQUtils(url: String, exchange: String) extends ConnectionFactory {

  setUri(url)
  private val connection = newConnection()
  private val channel = connection.createChannel()

  channel.exchangeDeclare(exchange, "direct", true)

  def publish[T](destination: String, entityId: String, value: T)(
      implicit encoder: Encoder[RabbitEnvelope[T]]
  ): Unit = {
    val envelope = RabbitEnvelope(entityId, value)
    channel.basicPublish(exchange, destination, null, envelope.asJson.noSpaces.getBytes)
  }

  def createNamedQueueFor(destination: String, queueName: String): Unit = {
    channel.queueDeclare(queueName, true, false, false, null)
    channel.queueBind(queueName, exchange, destination)
  }

  def createQueueFor(keyword: String): String = {
    val queueName = channel.queueDeclare().getQueue
    channel.queueBind(queueName, exchange, keyword)
    queueName
  }

  def getConsumerFor[T](
      queue: String
  )(implicit decoder: Decoder[T], ec: ExecutionContext): AMQPConsumer[T] =
    new AMQPConsumer[T](queue, channel)

}
