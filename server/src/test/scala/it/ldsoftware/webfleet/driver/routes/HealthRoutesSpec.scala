package it.ldsoftware.webfleet.driver.routes

import java.util.concurrent.CompletableFuture

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{OK, ServiceUnavailable}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.TopicPartition
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar._
import org.mockito.ArgumentMatchers._
import org.scalatest.{Matchers, WordSpec}
import spray.json.DefaultJsonProtocol

class HealthRoutesSpec extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with SprayJsonSupport
  with DefaultJsonProtocol {

  def mkRoutes(kp: KafkaProducer[String, String]): HealthRoutes = new HealthRoutes {
    override def kafkaProducer: KafkaProducer[String, String] = kp
  }

  "The health route" should {
    "Return a status of 200 and a map with all services statuses to ok" in {
      val ks = mock[KafkaProducer[String, String]]
      when(ks.send(any[ProducerRecord[String,String]]))
        .thenReturn(
          CompletableFuture
            .completedFuture(new RecordMetadata(new TopicPartition("waste", 0), 0L, 0L, 0L, 0L, 0, 0))
        )

      Get(HealthRoutes.healthPath) ~> Route.seal(mkRoutes(ks).healthRoute) ~> check {
        status shouldBe OK
        responseAs[Map[String, String]] shouldBe Map("kafka" -> "OK", "pgsql" -> "OK")
      }
    }

    "Return service unavailable when kafka is not working" in {
      val ks = mock[KafkaProducer[String, String]]
      val expected = "This should fail"
      when(ks.send(any[ProducerRecord[String,String]])).thenThrow(new Error(expected))

      Get(HealthRoutes.healthPath) ~> Route.seal(mkRoutes(ks).healthRoute) ~> check {
        status shouldBe ServiceUnavailable
        responseAs[Map[String, String]] shouldBe Map("kafka" -> expected, "pgsql" -> "OK")
      }
    }

    "Return service unavailable when postgresql is not working" in {

    }
  }

}
