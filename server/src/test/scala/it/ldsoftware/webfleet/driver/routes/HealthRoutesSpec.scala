package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{OK, ServiceUnavailable}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.ldsoftware.webfleet.driver.services.KafkaService
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{Matchers, WordSpec}
import spray.json.DefaultJsonProtocol

class HealthRoutesSpec extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with SprayJsonSupport
  with DefaultJsonProtocol {

  def mkRoutes(ks: KafkaService): HealthRoutes = new HealthRoutes {
    override def kafkaService: KafkaService = ks
  }

  "The health route" should {
    "Return a status of 200 and a map with all services statuses to ok" in {
      val ks = mock[KafkaService]
      when(ks.getHealth).thenReturn(OK)

      Get(HealthRoutes.healthPath) ~> Route.seal(mkRoutes(ks).healthRoute) ~> check {
        status shouldBe OK
        responseAs[Map[String, String]] shouldBe Map("webfleet-driver" -> OK.value, "kafka" -> OK.value)
      }
    }

    "Return service unavailable when kafka is not working" in {
      val ks = mock[KafkaService]
      when(ks.getHealth).thenReturn(ServiceUnavailable)

      Get(HealthRoutes.healthPath) ~> Route.seal(mkRoutes(ks).healthRoute) ~> check {
        status shouldBe ServiceUnavailable
        responseAs[Map[String, String]] shouldBe Map("webfleet-driver" -> OK.value, "kafka" -> ServiceUnavailable.value)
      }
    }
  }

}
