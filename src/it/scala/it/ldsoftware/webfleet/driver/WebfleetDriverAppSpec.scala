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
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.flows.DomainsFlow
import it.ldsoftware.webfleet.driver.flows.DomainsFlow._
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.security.{Permissions, User}
import it.ldsoftware.webfleet.driver.service.model.ApplicationHealth
import it.ldsoftware.webfleet.driver.testcontainers._
import it.ldsoftware.webfleet.driver.util.{RabbitEnvelope, RabbitMQUtils}
import it.ldsoftware.webfleet.driver.utils.ResponseUtils
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.Network

import scala.concurrent.{ExecutionContext, Future}

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

  lazy val auth0Server = new GenericMockContainer(network, provider, jwkKeyId)

  lazy val rabbit = new RabbitMQContainer(network)

  lazy val utils = new RabbitMQUtils("amqp://localhost", "webfleet")

  lazy val targetContainer =
    new TargetContainer(
      jdbcUrl = s"jdbc:postgresql://pgsql:5432/webfleet",
      globalNet = network
    )

  override val container: Container = MultipleContainers(pgsql, auth0Server, rabbit, targetContainer)

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
      Given("An user")
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      When("Creating the root of the website")
      val form = CreateForm(
        title = "Root of the website",
        path = "first-domain/",
        webType = Folder,
        description = "This is the root of the website",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      Then("The creation must be successful")
      val (status, headers) = createContent(form, jwt)

      status shouldBe StatusCodes.Created
      headers should contain(Location("first-domain/"))

      And("A get request should return the site details")
      val get = HttpRequest(uri = "http://localhost:8080/api/v1/contents/first-domain/")
        .withHeaders(Seq(jwt))
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
        path = "first-domain/",
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
            uri = "http://localhost:8080/api/v1/contents/first-domain/",
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
        ValidationError("path", "Content at first-domain/ already exists", "path.duplicate")
      )

    }
  }

  Feature("The application exposes a search endpoint") {
    Scenario("Searching content by path") {
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val uri = Uri("http://localhost:8080/api/v1/search/first-domain")
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

      val mainDomain = CreateForm(
        title = "Main domain",
        path = "by-title/",
        webType = Folder,
        description = "Main domain for search like test",
        text = "",
        contentStatus = Some(Published)
      )

      val form1 = CreateForm(
        title = "Some child",
        path = "by-title/child1",
        webType = Page,
        description = "First child",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val form2 = CreateForm(
        title = "Another child",
        path = "by-title/child2",
        webType = Page,
        description = "Second child",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      createContent(mainDomain, jwt)
      createContent(form1, jwt)
      createContent(form2, jwt)

      val uri = Uri("http://localhost:8080/api/v1/search/by-title")
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

      val domain = CreateForm(
        title = "By parent domain",
        path = "search-domain/",
        webType = Folder,
        description = "Search by parent main domain",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val form1 = CreateForm(
        title = "Base folder",
        path = "search-domain/base",
        webType = Folder,
        description = "First child",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val form2 = CreateForm(
        title = "Base child",
        path = "search-domain/base/child1",
        webType = Page,
        description = "Child of the base folder",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      val form3 = CreateForm(
        title = "Another base child",
        path = "search-domain/base/child2",
        webType = Page,
        description = "Second child of the base folder",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      createContent(domain, jwt)
      createContent(form1, jwt)
      createContent(form2, jwt)
      createContent(form3, jwt)

      val uri = Uri("http://localhost:8080/api/v1/search/search-domain")
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

  Feature("The application sends data to a rabbitmq queue") {
    Scenario("When an operation is executed, data is published on the exchange") {
      val queue = utils.createQueueFor("webfleet-contents")

      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val form = CreateForm(
        title = "A new content",
        path = "any-domain/",
        webType = Folder,
        description = "This is a new content",
        text = "Some sample text",
        contentStatus = Some(Published)
      )

      createContent(form, jwt)

      var actual: Option[RabbitEnvelope[Content.Event]] = None
      utils.getConsumerFor[Content.Event](queue).consume {
        case Left(error) =>
          logger.error(s"Error while consuming", error)
          Future.successful(akka.Done)
        case Right(value) =>
          actual = Some(value)
          Future.successful(akka.Done)
      }

      eventually {
        actual shouldBe defined
        val resp = actual.get
        resp.entityId shouldBe form.path
        resp.content shouldBe a[Content.Created]
        resp.content.asInstanceOf[Content.Created].form shouldBe form
      }
    }
  }

  Feature("The application asks to another service the permissions of an user for a domain") {
    Scenario("The user interacts with a domain he is allowed to operate in") {
      Given("A website")
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)
      val form = CreateForm(
        title = "Shared website",
        path = "shared-website/",
        webType = Folder,
        description = "This is the root of the shared website",
        text = "",
        contentStatus = Some(Published)
      )
      createContent(form, jwt)

      And("An user with write permissions on the website (see the mocked-replies.json)")
      val sharedUser = auth0Server.jwtHeader("tenant-user", Set())

      When("That user tries to create a page")
      val page = CreateForm(
        title = "Page created by shared user",
        path = "shared-website/page",
        webType = Page,
        description = "A page created by an user with whom I shared the website",
        text = "",
        contentStatus = Some(Published)
      )
      val (statusCode, _) = createContent(page, sharedUser)
      statusCode shouldBe StatusCodes.Created
    }
  }

  Feature("The application responds to changes in domains") {
    Scenario("An user creates a new domain and the root of that website is created") {
      Given("An user")
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      When("A domain created event is published on the domains topic")
      val form = DomainCreateForm("Website name", "from-topic", "icon.png")
      val event: DomainsFlow.Event =
        DomainsFlow.Created(form, User("superuser", Permissions.AllPermissions, None))

      utils.publish("webfleet-domains", form.id, event)

      Then("The application creates the root of that website with default values")
      eventually {
        val get = HttpRequest(uri = "http://localhost:8080/api/v1/contents/from-topic/")
          .withHeaders(Seq(jwt))
        val content = http
          .singleRequest(get)
          .flatMap(Unmarshal(_).to[WebContent])
          .futureValue

        content.title shouldBe "Website name"
        content.status shouldBe Published
      }
    }

    Scenario("An user deletes a domain and the root of that website is deleted") {
      Given("An user")
      val user = User("superuser", Permissions.AllPermissions, None)
      val jwt = auth0Server.jwtHeader(user.name, user.permissions)

      And("A domain")

      val form = DomainCreateForm("Website name", "delete-domain", "icon.png")
      val event: DomainsFlow.Event = DomainsFlow.Created(form, user)

      utils.publish("webfleet-domains", form.id, event)

      eventually {
        val get = HttpRequest(uri = "http://localhost:8080/api/v1/contents/delete-domain/")
          .withHeaders(Seq(jwt))
        val content = http
          .singleRequest(get)
          .flatMap(Unmarshal(_).to[WebContent])
          .futureValue

        content.title shouldBe "Website name"
        content.status shouldBe Published
      }

      When("The domain is deleted")
      val delete: DomainsFlow.Event = DomainsFlow.Deleted(user)

      utils.publish("webfleet-domains", form.id, delete)

      Then("The application removes the root of that website")

      eventually {
        val get = HttpRequest(uri = "http://localhost:8080/api/v1/contents/delete-domain/")
          .withHeaders(Seq(jwt))

        http
          .singleRequest(get)
          .map(r => r.status)
          .futureValue shouldBe StatusCodes.NotFound
      }
    }
  }

  def createContent(form: CreateForm, jwt: HttpHeader): (StatusCode, Seq[HttpHeader]) = {
    Marshal(form)
      .to[RequestEntity]
      .map(e =>
        HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:8080/api/v1/contents/${form.path}",
          entity = e
        ).withHeaders(Seq(jwt))
      )
      .map(r => http.singleRequest(r))
      .flatMap(f => f.map(resp => (resp.status, resp.headers)))
      .futureValue
  }
}
