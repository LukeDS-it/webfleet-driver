package it.ldsoftware.webfleet.driver

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import com.dimafeng.testcontainers.{Container, ForAllTestContainer}
import it.ldsoftware.webfleet.driver.http.model.NamedEntity
import it.ldsoftware.webfleet.driver.testcontainers.TargetContainer
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

class WebfleetDriverAppSpec
  extends AnyFeatureSpec
    with GivenWhenThen
    with Matchers
    with ForAllTestContainer
    with ScalaFutures
    with IntegrationPatience
    with FailFastCirceSupport {

  implicit val ec: ExecutionContext = ExecutionContext.global
  lazy val targetContainer = new TargetContainer(dockerHost = sys.env.getOrElse("IP_DOCKER_HOST", "customHostName"))

  override val container: Container = targetContainer

  override def afterStart(): Unit = {
    targetContainer.enableLogging()
  }

  implicit lazy val system: ActorSystem = ActorSystem("test-microservice-quickstart")
  implicit lazy val materializer: Materializer = Materializer(system)
  lazy val http: HttpExt = Http(system)

  Feature("The application sends a greeting") {
    Scenario("The application greets the world when called in GET") {
      val r = HttpRequest(uri = s"http://localhost:8080")
      val result = http
        .singleRequest(r)
        .flatMap(Unmarshal(_).to[String])
        .futureValue

      result shouldBe "Hello world!"
    }

    Scenario("The application greets the person when called in POST with a name specified") {
      val result = Marshal(NamedEntity("Joe"))
        .to[RequestEntity]
        .map { e =>
          HttpRequest(uri = s"http://localhost:8080", method = HttpMethods.POST, entity = e)
        }
        .flatMap(req => http.singleRequest(req))
        .flatMap(Unmarshal(_).to[String])
        .futureValue

      result shouldBe "Hello Joe"
    }
  }
}
