package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.ldsoftware.webfleet.api.v1.auth.Principal
import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.routes.utils.{AggregateFormatting, PrincipalExtractor}
import it.ldsoftware.webfleet.driver.services.repositories.AggregateRepository
import it.ldsoftware.webfleet.driver.services.utils.AuthenticationUtils
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

  def mkRoutes(svc: AggregateDriverV1, repo: AggregateRepository, extr: PrincipalExtractor): AggregateRoutes =
    new AggregateRoutes {
      override def aggregateDriver: AggregateDriverV1 = svc

      override def aggregateRepo: AggregateRepository = repo

      override def extractor: PrincipalExtractor = extr
    }

  val agg = Aggregate(Some("programs"), Some("Programs section"), Some("This is the program section"))
  val jwt = "test-jwt"

  "The post aggregate route" should {
    val aggregateRepo = mock[AggregateRepository]

    "Return 201 created when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(None, agg, fakePrincipal)).thenReturn(Created("programs"))

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Created
          entityAs[String] shouldBe "programs"
        }
    }

    "Return 400 with the list of errors when aggregate validation fails" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))
      val expectedError = FieldError("name", "Aggregate with this name already exists")
      val expected = ValidationError(Set(expectedError))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(None, agg, fakePrincipal)).thenReturn(expected)

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[FieldError]] should contain(expectedError)
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(aggregateSvc.addAggregate(None, agg, fakePrincipal)).thenReturn(NoContent)

      Post(AggregateRoutes.aggregatePath, agg) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).addAggregate(None, agg, fakePrincipal)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(None, agg, fakePrincipal)).thenReturn(ForbiddenError)

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(None, agg, fakePrincipal)).thenThrow(new Error())

      Post(AggregateRoutes.aggregatePath, agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }

  }

  "The post child aggregate route" should {
    val parent = "main"
    val aggregateRepo = mock[AggregateRepository]

    "Return 201 created when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(Some(parent), agg, fakePrincipal)).thenReturn(Created("programs"))

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Created
          entityAs[String] shouldBe "programs"
        }
    }

    "Return 400 with the list of errors when aggregate validation fails" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val expectedError = FieldError("name", "Aggregate with this name already exists")
      val expected = ValidationError(Set(expectedError))
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(Some(parent), agg, fakePrincipal)).thenReturn(expected)

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[FieldError]] should contain(expectedError)
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(Some(parent), agg, fakePrincipal)).thenReturn(NoContent)

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).addAggregate(Some(parent), agg, fakePrincipal)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(Some(parent), agg, fakePrincipal)).thenReturn(ForbiddenError)

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.addAggregate(Some(parent), agg, fakePrincipal)).thenThrow(new Error())

      Post(s"${AggregateRoutes.aggregatePath}/$parent", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "The put aggregate route" should {
    val target = "main"
    val aggregateRepo = mock[AggregateRepository]

    "Return 204 no content when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.editAggregate(target, agg, fakePrincipal)).thenReturn(NoContent)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NoContent
        }
    }

    "Return 400 with the list of errors when aggregate validation fails" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))
      val expectedError = FieldError("name", "Aggregate with this name already exists")
      val expected = ValidationError(Set(expectedError))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.editAggregate(target, agg, fakePrincipal)).thenReturn(expected)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[FieldError]] should contain(expectedError)
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.editAggregate(target, agg, fakePrincipal)).thenReturn(NoContent)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).editAggregate(target, agg, fakePrincipal)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.editAggregate(target, agg, fakePrincipal)).thenReturn(ForbiddenError)

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(aggregateSvc.editAggregate(target, agg, fakePrincipal)).thenThrow(new Error())

      Put(s"${AggregateRoutes.aggregatePath}/$target", agg) ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "The delete aggregate route" should {
    val target = "main"
    val aggregateRepo = mock[AggregateRepository]

    "Return 204 no content when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.deleteAggregate(target, fakePrincipal)).thenReturn(NoContent)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NoContent
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.deleteAggregate(target, fakePrincipal)).thenReturn(NoContent)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).deleteAggregate(target, fakePrincipal)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.deleteAggregate(target, fakePrincipal)).thenReturn(ForbiddenError)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 404 when the aggregate is not present" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.deleteAggregate(target, fakePrincipal)).thenReturn(NotFoundError)

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NotFound
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(aggregateSvc.deleteAggregate(target, fakePrincipal)).thenThrow(new Error())

      Delete(s"${AggregateRoutes.aggregatePath}/$target") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }

  }

  "The move aggregate route" should {
    val (target, to) = ("from", "to")
    val aggregateRepo = mock[AggregateRepository]

    "Return 204 no content when the operation completes successfully" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.moveAggregate(target, to, fakePrincipal)).thenReturn(NoContent)

      Post(s"${AggregateRoutes.aggregatePath}/$target/move/$to") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NoContent
        }
    }

    "Return 400 bad request if the target destination does not exist" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))
      val expectedError = FieldError("destination", "Target aggregate does not exist")
      val expected = ValidationError(Set(expectedError))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.moveAggregate(target, to, fakePrincipal)).thenReturn(expected)

      Post(s"${AggregateRoutes.aggregatePath}/$target/move/$to") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[FieldError]] should contain(expectedError)
        }
    }

    "Return 401 when the user is not authenticated" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.moveAggregate(target, to, fakePrincipal)).thenReturn(NoContent)

      Post(s"${AggregateRoutes.aggregatePath}/$target/move/$to") ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }

      verify(aggregateSvc, never).moveAggregate(target, to, fakePrincipal)
    }

    "Return 403 when the user cannot access the resource" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.moveAggregate(target, to, fakePrincipal)).thenReturn(ForbiddenError)

      Post(s"${AggregateRoutes.aggregatePath}/$target/move/$to") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }

    "Return 404 when the aggregate is not present" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.moveAggregate(target, to, fakePrincipal)).thenReturn(NotFoundError)

      Post(s"${AggregateRoutes.aggregatePath}/$target/move/$to") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.NotFound
        }
    }

    "Return 500 when there is an unexpected error" in {
      val aggregateSvc = mock[AggregateDriverV1]
      val extractor = mock[PrincipalExtractor]
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      when(extractor.extractPrincipal(jwt)).thenReturn(Some(fakePrincipal))
      when(aggregateSvc.moveAggregate(target, to, fakePrincipal)).thenThrow(new Error())

      Post(s"${AggregateRoutes.aggregatePath}/$target/move/$to") ~>
        addCredentials(OAuth2BearerToken(jwt)) ~>
        Route.seal(mkRoutes(aggregateSvc, aggregateRepo, extractor).aggregateRoutes) ~>
        check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }
}
