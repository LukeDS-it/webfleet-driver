package it.ldsoftware.webfleet.driver

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, MultipleContainers}
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.security.Permissions
import it.ldsoftware.webfleet.driver.service.model.ApplicationHealth
import it.ldsoftware.webfleet.driver.testcontainers.{Auth0MockContainer, PgsqlContainer, TargetContainer}
import it.ldsoftware.webfleet.driver.utils.ResponseUtils
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
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
    with ResponseUtils
    with Eventually {

  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val jwkKeyId = "0i2mNOFBoWVO7IPdkZr1aeBSJz0yyUEu5h1jT85Hc8XOKMftXgV37R"
  lazy val provider: JwkProvider = new JwkProviderBuilder("http://localhost:9999").build()

  val network: Network = Network.newNetwork()

  lazy val pgsql = new PgsqlContainer(network)

  lazy val auth0Server = new Auth0MockContainer(network, provider, jwkKeyId)

  lazy val targetContainer =
    new TargetContainer(
      jdbcUrl = s"jdbc:postgresql://pgsql:5432/webfleet",
      globalNet = network
    )

  override val container: Container = MultipleContainers(pgsql, auth0Server, targetContainer)

  implicit lazy val system: ActorSystem = ActorSystem("test-webfleet-driver")
  implicit lazy val materializer: Materializer = Materializer(system)
  lazy val http: HttpExt = Http(system)

  lazy val db: Database = Database.forConfig(
    "slick.db",
    ConfigFactory.parseString(s"""
      |slick {
      |  profile = "slick.jdbc.PostgresProfile$$"
      |  db {
      |    url = "jdbc:postgresql://localhost:${pgsql.mappedPort(5432)}/webfleet"
      |    user = "webfleet"
      |    password = "password"
      |    driver = "org.postgresql.Driver"
      |    numThreads = 5
      |    maxConnections = 5
      |    minConnections = 1
      |    connectionTimeout = 3 seconds
      |  }
      |}
      |""".stripMargin)
  )

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
    Scenario("The user sends a valid creation request and is executed successfully") {
      val form = CreateForm(
        title = "Root of the website",
        path = "/",
        webType = Folder,
        description = "This is the root of the website",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val (status, headers) = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(
            method = HttpMethods.POST,
            uri = "http://localhost:8080/api/v1/contents/",
            entity = e
          ).withHeaders(Seq(jwt))
        )
        .map(r => http.singleRequest(r))
        .flatMap(f => f.map(resp => (resp.status, resp.headers)))
        .futureValue

      status shouldBe StatusCodes.Created
      headers should contain(Location("/"))

      val get = HttpRequest(uri = "http://localhost:8080/api/v1/contents/").withHeaders(Seq(jwt))
      val content = http
        .singleRequest(get)
        .flatMap(Unmarshal(_).to[WebContent])
        .futureValue

      content.title shouldBe "Root of the website"
      content.status shouldBe Published
    }

    Scenario("The user sends an invalid creation request and is rejected with an explanation") {
      val form = CreateForm(
        title = "Root of the website",
        path = "/",
        webType = Folder,
        description = "This is the root of the website",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val (status, content) = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(
            method = HttpMethods.POST,
            uri = "http://localhost:8080/api/v1/contents/",
            entity = e
          ).withHeaders(Seq(jwt))
        )
        .map(r => http.singleRequest(r))
        .flatMap(f =>
          f.flatMap(resp => Unmarshal(resp).to[List[ValidationError]].map(x => (resp.status, x)))
        )
        .futureValue

      status shouldBe StatusCodes.BadRequest
      content shouldBe List(
        ValidationError("path", "Content at / already exists", "path.duplicate")
      )

    }
  }

  Feature("The application has a read side that is updated every time there is an event") {
    Scenario("Adding a content causes the read side to be updated") {

    }
  }

  Feature("The application exposes a search endpoint") {
    Scenario("Searching content by path") {
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val uri = Uri("http://localhost:8080/api/v1/search")
        .withQuery(Uri.Query("path" -> "/"))

      val resp = http
        .singleRequest(HttpRequest(uri = uri).withHeaders(Seq(jwt)))
        .flatMap(Unmarshal(_).to[List[ContentRM]])
        .futureValue

      resp should have size 1
      resp.head.description shouldBe "This is the root of the website"
    }

    Scenario("Searching content by title like") {}

    Scenario("Searching content by parent") {}
  }
}
