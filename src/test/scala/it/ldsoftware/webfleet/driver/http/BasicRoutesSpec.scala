package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.ldsoftware.webfleet.driver.http.model.NamedEntity
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import io.circe.generic.auto._

class BasicRoutesSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with ScalatestRouteTest
  with MockitoSugar
  with FailFastCirceSupport {

  // As soon as the routes depend on services, put them as arguments in the function and override them directly
  // Then mock them in each test using mock[Service]
  def routes(): BasicRoutes = new BasicRoutes {

  }

  "The root path" should {
    "return a greeting" in {
      val request = HttpRequest(uri = "/")

      request ~> routes().routes ~> check {
        status shouldBe StatusCodes.OK
        entityAs[String] shouldBe "Hello world!"
      }
    }
  }

  "The post root path" should {
    "return a greeting" in {
      val request = Marshal(NamedEntity("Joe"))
        .to[RequestEntity]
        .map(e => HttpRequest(uri = "/", method = HttpMethods.POST, entity = e))
        .futureValue

      request ~> routes().routes ~> check {
        status shouldBe StatusCodes.OK
        entityAs[String] shouldBe "Hello Joe"
      }
    }
  }

}
