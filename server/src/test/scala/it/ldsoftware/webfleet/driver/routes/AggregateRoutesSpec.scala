package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.routes.utils.AggregateFormatting
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import spray.json.RootJsonFormat

class AggregateRoutesSpec extends WordSpec
  with Matchers
  with MockitoSugar
  with ScalatestRouteTest
  with AggregateFormatting {

  implicit val FieldErrorFormatter: RootJsonFormat[FieldError] = jsonFormat2(FieldError)

  def mkRoutes(svc: AggregateDriverV1): AggregateRoutes = new AggregateRoutes {
    override def aggregateService: AggregateDriverV1 = svc
  }

  val agg = Aggregate(Some("programs"), Some("Programs section"), Some("This is the program section"))
  val jwt = "test-jwt"

  "The post aggregate route" should {

    "Return 201 created when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(None, agg, jwt)).thenReturn(Created("programs"))

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Created
          entityAs[String] shouldBe "programs"
        }
    }

    "Return 400 with the list of errors when aggregate validation fails" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val expectedError = FieldError("name", "Aggregate with this name already exists")
      val expected = ValidationError(List(expectedError))

      when(aggregateSvc.addAggregate(None, agg, jwt)).thenReturn(expected)

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[FieldError]] should contain(expectedError)
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(None, agg, jwt)).thenReturn(NoContent)

      Post(AggregateRoutes.aggregatePath, agg) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).addAggregate(None, agg, jwt)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(None, agg, jwt)).thenReturn(ForbiddenError)

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

  }


}
