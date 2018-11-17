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

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(None, agg, jwt)).thenThrow(new Error())

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }

  }

  "The post child aggregate route" should {
    val parent = "main"

    "Return 201 created when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(Some(parent), agg, jwt)).thenReturn(Created("programs"))

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
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

      when(aggregateSvc.addAggregate(Some(parent), agg, jwt)).thenReturn(expected)

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[FieldError]] should contain(expectedError)
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(Some(parent), agg, jwt)).thenReturn(NoContent)

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).addAggregate(Some(parent), agg, jwt)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(Some(parent), agg, jwt)).thenReturn(ForbiddenError)

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.addAggregate(Some(parent), agg, jwt)).thenThrow(new Error())

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "The put aggregate route" should {
    val target = "main"

    "Return 204 no content when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.editAggregate(target, agg, jwt)).thenReturn(NoContent)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NoContent
        }
    }

    "Return 400 with the list of errors when aggregate validation fails" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val expectedError = FieldError("name", "Aggregate with this name already exists")
      val expected = ValidationError(List(expectedError))

      when(aggregateSvc.editAggregate(target, agg, jwt)).thenReturn(expected)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[FieldError]] should contain(expectedError)
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.editAggregate(target, agg, jwt)).thenReturn(NoContent)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).editAggregate(target, agg, jwt)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.editAggregate(target, agg, jwt)).thenReturn(ForbiddenError)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.editAggregate(target, agg, jwt)).thenThrow(new Error())

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "The delete aggregate route" should {
    val target = "main"

    "Return 204 no content when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.deleteAggregate(target, jwt)).thenReturn(NoContent)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NoContent
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.deleteAggregate(target, jwt)).thenReturn(NoContent)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).deleteAggregate(target, jwt)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.deleteAggregate(target, jwt)).thenReturn(ForbiddenError)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 404 when the aggregate is not present" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.deleteAggregate(target, jwt)).thenReturn(NotFoundError)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NotFound
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]

      when(aggregateSvc.deleteAggregate(target, jwt)).thenThrow(new Error())

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }

  }
}
