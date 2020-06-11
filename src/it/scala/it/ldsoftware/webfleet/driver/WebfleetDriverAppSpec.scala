package it.ldsoftware.webfleet.driver

import java.time.Duration
import java.util.{Collections, Properties}

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
import it.ldsoftware.webfleet.driver.testcontainers._
import it.ldsoftware.webfleet.driver.utils.ResponseUtils
import org.apache.kafka.clients.consumer.KafkaConsumer
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

  lazy val kafka = new CustomKafkaContainer(network)

  lazy val targetContainer =
    new TargetContainer(
      jdbcUrl = s"jdbc:postgresql://pgsql:5432/webfleet",
      globalNet = network
    )

  override val container: Container = MultipleContainers(pgsql, auth0Server, kafka, targetContainer)

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

  Feature("The application allows content creation") {
    Scenario("The user sends a valid creation request and is executed successfully") {
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val form = CreateForm(
        title = "Root of the website",
        path = "/",
        webType = Folder,
        description = "This is the root of the website",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val (status, headers) = createContent(form, jwt)

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

  Feature("The application exposes a search endpoint") {
    Scenario("Searching content by path") {
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val uri = Uri("http://localhost:8080/api/v1/search")
        .withQuery(Uri.Query("path" -> "/"))

      eventually {
        val resp = http
          .singleRequest(HttpRequest(uri = uri).withHeaders(Seq(jwt)))
          .flatMap(Unmarshal(_).to[List[ContentRM]])
          .futureValue

        resp should have size 1
        resp.head.description shouldBe "This is the root of the website"
      }
    }

    Scenario("Searching content by title like") {
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val form1 = CreateForm(
        title = "Some child",
        path = "/child1",
        webType = Page,
        description = "First child",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val form2 = CreateForm(
        title = "Another child",
        path = "/child2",
        webType = Page,
        description = "Second child",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      createContent(form1, jwt)
      createContent(form2, jwt)

      val uri = Uri("http://localhost:8080/api/v1/search")
        .withQuery(Uri.Query("title" -> "child"))

      eventually {
        val resp = http
          .singleRequest(HttpRequest(uri = uri).withHeaders(Seq(jwt)))
          .flatMap(Unmarshal(_).to[List[ContentRM]])
          .futureValue

        resp should have size 2
      }
    }

    Scenario("Searching content by parent") {
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val form1 = CreateForm(
        title = "Base folder",
        path = "/base",
        webType = Folder,
        description = "First child",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val form2 = CreateForm(
        title = "Base child",
        path = "/base/child1",
        webType = Page,
        description = "Child of the base folder",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val form3 = CreateForm(
        title = "Another base child",
        path = "/base/child2",
        webType = Page,
        description = "Second child of the base folder",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      createContent(form1, jwt)
      createContent(form2, jwt)
      createContent(form3, jwt)

      val uri = Uri("http://localhost:8080/api/v1/search")
        .withQuery(Uri.Query("parent" -> "/base"))

      eventually {
        val resp = http
          .singleRequest(HttpRequest(uri = uri).withHeaders(Seq(jwt)))
          .flatMap(Unmarshal(_).to[List[ContentRM]])
          .futureValue

        resp should have size 2
      }
    }
  }

  Feature("The application sends data to a kafka topic") {
    Scenario("When an operation is executed, data is published on the topic") {
      val props: Properties = new Properties()
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("bootstrap.servers", s"http://localhost:${kafka.mappedPort(9093)}")
      props.put("group.id", "webfleet-test")
      props.put("enable.auto.commit", "true")

      val kafkaConsumer = new KafkaConsumer[String, String](props)
      kafkaConsumer.subscribe(Collections.singletonList("webfleet-contents"))

      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val form = CreateForm(
        title = "A new content",
        path = "/a-new-content",
        webType = Folder,
        description = "This is a new content",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      createContent(form, jwt)

      eventually {
        val records = kafkaConsumer.poll(Duration.ofSeconds(1L))
        records.count() should be >= 1
      }
    }
  }

  def createContent(form: CreateForm, jwt: HttpHeader): (StatusCode, Seq[HttpHeader]) = {
    Marshal(form)
      .to[RequestEntity]
      .map(e =>
        HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:8080/api/v1/contents${form.path}",
          entity = e
        ).withHeaders(Seq(jwt))
      )
      .map(r => http.singleRequest(r))
      .flatMap(f => f.map(resp => (resp.status, resp.headers)))
      .futureValue
  }
}
