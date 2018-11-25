package it.ldsoftware.webfleet.driver.routes

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.driver.conf.ApplicationProperties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import spray.json.DefaultJsonProtocol

import scala.util.{Failure, Success, Try}

trait HealthRoutes extends SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  def kafkaProducer: KafkaProducer[String, String]

  def healthRoute: Route = path("api" / "v1" / "health") {
    get {
      complete {
        val (kfStatus, kfDetail) = getKafkaStatus
        val (pgStatus, pgDetail) = getPgStatus

        val defStatus = if (kfStatus.isFailure || pgStatus.isFailure)
          StatusCodes.ServiceUnavailable
        else
          StatusCodes.OK

        defStatus -> Map("kafka" -> kfDetail, "pgsql" -> pgDetail)
      }
    }
  }

  def getKafkaStatus: (StatusCode, String) = {
    val data = HealthRoutes.rndData
    Try {
      kafkaProducer.send(data).get()
    } match {
      case Success(_) => (StatusCodes.OK, "OK")
      case Failure(ex) =>
        logger.error("Kafka is unavailable", ex)
        (StatusCodes.ServiceUnavailable, ex.getMessage)
    }
  }

  def getPgStatus: (StatusCode, String) = (StatusCodes.OK, "OK")

}

object HealthRoutes {
  val healthPath: String = "/api/v1/health"
  val pingValue = "ping"
  lazy val TopicName = s"${ApplicationProperties.topicPrefix}waste"

  def rndData: ProducerRecord[String, String] =
    new ProducerRecord[String, String](TopicName, UUID.randomUUID().toString, pingValue)
}