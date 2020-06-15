package it.ldsoftware.webfleet.driver.http

import java.time.ZonedDateTime

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.Folder
import it.ldsoftware.webfleet.driver.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentReadService
import it.ldsoftware.webfleet.driver.service.model._
import org.mockito.Mockito._

import scala.concurrent.Future

class SearchRoutesSpec extends BaseHttpSpec {

  "The GET path" should {
    "return a list of contents for a particular domain" in {
      val uri = Uri("/api/v1/search/domain").withQuery(Query("path" -> "/"))

      val svc = mock[ContentReadService]
      val expected = List(ContentRM("/", "a", "b", Folder, ZonedDateTime.now, None))

      when(svc.search(ContentFilter(Some("domain/"), None, None)))
        .thenReturn(Future.successful(success(expected)))

      when(defaultExtractor.extractUser(CorrectJWT, None))
        .thenReturn(Future.successful(Some(User("me", Set(), Some(CorrectJWT)))))

      HttpRequest(uri = uri) ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new SearchRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[List[ContentRM]] shouldBe expected
        }
    }

    "correctly search domain children" in {
      val uri = Uri("/api/v1/search/domain").withQuery(Query("path" -> "/child/of/child"))

      val svc = mock[ContentReadService]
      val expected = List(ContentRM("/", "a", "b", Folder, ZonedDateTime.now, None))

      when(svc.search(ContentFilter(Some("domain/child/of/child"), None, None)))
        .thenReturn(Future.successful(success(expected)))

      when(defaultExtractor.extractUser(CorrectJWT, None))
        .thenReturn(Future.successful(Some(User("me", Set(), Some(CorrectJWT)))))

      HttpRequest(uri = uri) ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new SearchRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[List[ContentRM]] shouldBe expected
        }
    }
  }

}
