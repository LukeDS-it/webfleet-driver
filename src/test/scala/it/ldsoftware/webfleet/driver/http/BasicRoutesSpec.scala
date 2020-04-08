package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.http.model.NamedEntity
import it.ldsoftware.webfleet.driver.service.{GreeterService, HealthService}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._

import scala.concurrent.Future

class BasicRoutesSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with MockitoSugar
    with FailFastCirceSupport {

  def routes(greeterService: GreeterService, healthService: HealthService): BasicRoutes =
    new BasicRoutes(greeterService, healthService)

  "The root path" should {

    "return a greeting" in {
      val request = HttpRequest(uri = "/")
      val greeter = mock[GreeterService]
      val health = mock[HealthService]

      request ~> routes(greeter, health).routes ~> check {
        status shouldBe StatusCodes.OK
        entityAs[String] shouldBe "Hello world!"
      }
    }
  }

  "The post root path" should {
    "return a greeting" in {
      val greeter = mock[GreeterService]
      val health = mock[HealthService]
      when(greeter.greet("Joe")).thenReturn(Future.successful("Hello Joe"))

      val request = Marshal(NamedEntity("Joe"))
        .to[RequestEntity]
        .map(e => HttpRequest(uri = "/", method = HttpMethods.POST, entity = e))
        .futureValue

      request ~> routes(greeter, health).routes ~> check {
        status shouldBe StatusCodes.OK
        entityAs[String] shouldBe "Hello Joe"
      }
    }
  }

}
