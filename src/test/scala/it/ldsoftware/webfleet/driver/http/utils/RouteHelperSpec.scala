package it.ldsoftware.webfleet.driver.http.utils

import akka.http.scaladsl.model.headers.{Location, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.ValidationError
import it.ldsoftware.webfleet.driver.http.model.out.RestError
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.model._
import org.mockito.Mockito._

import scala.concurrent.Future

class RouteHelperSpec extends BaseHttpSpec with RouteHelper {

  val extractor: UserExtractor = mock[UserExtractor]

  val testRoutes: Route =
    path("succeed") {
      get {
        svcCall[String](Future.successful(success("OK")))
      } ~ post {
        svcCall[String](Future.successful(created("path/to/resource")))
      } ~ put {
        svcCall[NoResult, NoResult](Future.successful(noOutput), Identity)
      }
    } ~ pathPrefix("accept") {
      post {
        svcCall[NoResult](Future.successful(accepted))
      }
    } ~ pathPrefix("fail") {
      path("bad-request") {
        post {
          svcCall[NoResult](
            Future.successful(invalid(List(ValidationError("field", "error", "fld.err"))))
          )
        }
      } ~ path("not-found") {
        svcCall[NoResult](Future.successful(notFound("not-found")))
      } ~ path("internal-server-error") {
        svcCall[NoResult](Future.successful(unexpectedError(new Exception(), "Message")))
      }
    } ~ path("authenticated") {
      login { user =>
        svcCall[User](Future {
          if (user.permissions.contains("pass")) success(user)
          else forbidden
        })
      }
    }

  "The completeWith method" should {
    "return success data when the service call succeeds" in {
      val request = HttpRequest(uri = "/succeed")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.OK
        entityAs[String] shouldBe "OK"
      }
    }

    "return Created when the service call succeeds with creation" in {
      val request = HttpRequest(uri = "/succeed", method = HttpMethods.POST)

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.Created
        header("Location") shouldBe Some(Location("path/to/resource"))
      }
    }

    "return No Content when the service call succeeds with no return data" in {
      val request = HttpRequest(uri = "/succeed", method = HttpMethods.PUT)

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "return Accepted when the service call is asynchronous and accepts the request" in {
      val request = HttpRequest(uri = "/accept", method = HttpMethods.POST)

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.Accepted
      }
    }

    "return Bad Request when the service call rejects invalid data" in {
      val request = HttpRequest(uri = "/fail/bad-request", method = HttpMethods.POST)

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.BadRequest
        entityAs[List[ValidationError]] shouldBe List(ValidationError("field", "error", "fld.err"))
      }
    }

    "return Not Found when the service does not find the target resource" in {
      val request = HttpRequest(uri = "/fail/not-found")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.NotFound
        entityAs[RestError] shouldBe RestError("Requested resource not-found could not be found")
      }
    }

    "return an unexpected error when the service fails without specific information" in {
      val request = HttpRequest(uri = "/fail/internal-server-error")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.InternalServerError
        entityAs[RestError] shouldBe RestError("Message")
      }
    }
  }

  "The authentication utility" should {
    "correctly recognise a valid user" in {
      val user = User("name", Set("pass"), Some(CorrectJWT))
      when(extractor.extractUser(CorrectJWT)).thenReturn(Some(user))

      val request = HttpRequest(uri = "/authenticated", headers = List())

      request ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~> testRoutes ~> check {
        status shouldBe StatusCodes.OK
        entityAs[User] shouldBe user
      }
    }

    "refuse an user with wrong permissions" in {
      when(extractor.extractUser(WrongJWT))
        .thenReturn(Some(User("name", Set("wrong"), Some(WrongJWT))))

      val request = HttpRequest(uri = "/authenticated")

      request ~> addCredentials(OAuth2BearerToken(WrongJWT)) ~> testRoutes ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    "refuse requests with incorrect JWT" in {
      when(extractor.extractUser(WrongJWT)).thenReturn(None)

      val request = HttpRequest(uri = "/authenticated")

      request ~> addCredentials(OAuth2BearerToken(WrongJWT)) ~> testRoutes ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "refuse requests without JWT" in {
      val request = HttpRequest(uri = "/authenticated")

      request ~> testRoutes ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }
  }

}
