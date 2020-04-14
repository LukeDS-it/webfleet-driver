package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.{Folder, Published, WebContent}
import it.ldsoftware.webfleet.driver.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentService
import it.ldsoftware.webfleet.driver.service.model._
import org.mockito.Mockito._

import scala.concurrent.Future

class ContentRoutesSpec extends BaseHttpSpec {

  "The GET path" should {
    "return the information of the root" in {
      val request = HttpRequest(uri = "/api/v1/contents/")
      val svc = mock[ContentService]
      val expectedContent = WebContent(
        "title",
        "path",
        Folder,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Published,
        "",
        None,
        None,
        Map()
      )

      when(svc.getContent("/")).thenReturn(Future.successful(success(expectedContent)))
      when(defaultExtractor.extractUser(CorrectJWT))
        .thenReturn(Some(User("me", Set(), Some(CorrectJWT))))

      request ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[WebContent] shouldBe expectedContent
        }
    }

    "return the information of any content" in {
      val request = HttpRequest(uri = "/api/v1/contents/content/path")
      val svc = mock[ContentService]
      val expectedContent = WebContent(
        "title",
        "path",
        Folder,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Published,
        "",
        None,
        None,
        Map()
      )

      when(svc.getContent("/content/path")).thenReturn(Future.successful(success(expectedContent)))
      when(defaultExtractor.extractUser(CorrectJWT))
        .thenReturn(Some(User("me", Set(), Some(CorrectJWT))))

      request ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[WebContent] shouldBe expectedContent
        }
    }
  }

  "The POST path" should {
    "return a Created response when anything has been created" in {}

    "return with a 400 if the form data is invalid" in {}
  }

  "The PUT path" should {
    "return with a No Content response when anything has been updated" in {}

    "return with a 400 if the form data is invalid" in {}
  }

  "The DELETE path" should {
    "return with a No Content response when anything has been deleted" in {}
  }

}
