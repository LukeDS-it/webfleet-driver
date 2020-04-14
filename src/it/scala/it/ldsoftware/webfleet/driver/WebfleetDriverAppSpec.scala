package it.ldsoftware.webfleet.driver

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, MultipleContainers}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.service.model.ApplicationHealth
import it.ldsoftware.webfleet.driver.testcontainers.{PgsqlContainer, TargetContainer}
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.Network

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

  val network: Network = Network.newNetwork()

  lazy val pgsql = new PgsqlContainer(network)

  lazy val targetContainer =
    new TargetContainer(
      jdbcUrl = s"jdbc:postgresql://pgsql:5432/webfleet",
      globalNet = network
    )

  override val container: Container = MultipleContainers(pgsql, targetContainer)

  implicit lazy val system: ActorSystem = ActorSystem("test-webfleet-driver")
  implicit lazy val materializer: Materializer = Materializer(system)
  lazy val http: HttpExt = Http(system)

  Feature("The application exposes a healthcheck address") {
    Scenario("The application sends an OK response when everything works fine") {
      val r = HttpRequest(uri = s"http://localhost:8080/health")
      val result = http
        .singleRequest(r)
        .flatMap(Unmarshal(_).to[ApplicationHealth])
        .futureValue

      result shouldBe ApplicationHealth("ok")
    }
  }

}
