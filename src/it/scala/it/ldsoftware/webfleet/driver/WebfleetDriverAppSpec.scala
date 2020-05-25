package it.ldsoftware.webfleet.driver

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, MultipleContainers}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, Folder}
import it.ldsoftware.webfleet.driver.security.Permissions
import it.ldsoftware.webfleet.driver.service.model.ApplicationHealth
import it.ldsoftware.webfleet.driver.testcontainers.{PgsqlContainer, TargetContainer}
import it.ldsoftware.webfleet.driver.utils.{AuthUtils, ResponseUtils}
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
    with FailFastCirceSupport
    with AuthUtils
    with ResponseUtils {

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

  Feature("As an user, I want to add content to my website") {
    Scenario("The user sends a valid creation request") {
      val form = CreateForm(
        title = "Root of the website",
        path = "/",
        webType = Folder,
        description = "This is the root of the website",
        text = "Some sample text"
      )

      val jwt = jwtHeader("superuser", Permissions.AllPermissions)

      val response = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(
            method = HttpMethods.POST,
            uri = "http://localhost:8080/api/v1/contents/",
            entity = e
          ).withHeaders(Seq(jwt))
        )
        .map(r => http.singleRequest(r))
//        .flatMap(resp => resp.flatMap(e => e.toStrict(1.second)))
//        .futureValue

      debug(response)
    }
  }

}
