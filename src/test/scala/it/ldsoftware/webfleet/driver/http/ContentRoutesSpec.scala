package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Location, OAuth2BearerToken}
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.http.RestError
import it.ldsoftware.webfleet.commons.security.User
import it.ldsoftware.webfleet.commons.service.model._
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.driver.security.Permissions
import it.ldsoftware.webfleet.driver.service.ContentService
import org.mockito.Mockito._

import scala.concurrent.Future

class ContentRoutesSpec extends BaseHttpSpec {

  "The GET path" should {
    "return the information of the root of a domain" in {
      val request = HttpRequest(uri = "/api/v1/contents/domain/")
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

      val user = User("me", Permissions.AllPermissions, Some(CorrectJWT))

      when(svc.getContent("domain", "/")).thenReturn(Future.successful(success(expectedContent)))
      when(defaultExtractor.extractUser(CorrectJWT, Some("domain")))
        .thenReturn(Future.successful(Some(user)))

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

      val user = User("me", Permissions.AllPermissions, Some(CorrectJWT))

      when(svc.getContent("content", "/path"))
        .thenReturn(Future.successful(success(expectedContent)))
      when(defaultExtractor.extractUser(CorrectJWT, Some("content")))
        .thenReturn(Future.successful(Some(user)))

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
    "return a Created response when anything has been created" in {
      val form =
        CreateForm("title", "path", Folder, "description", "text", "theme", "icon", None, None)

      val user = User("user", Set(Permissions.Contents.Create), Some(CorrectJWT))
      when(defaultExtractor.extractUser(CorrectJWT, Some("domain")))
        .thenReturn(Future.successful(Some(user)))

      val svc = mock[ContentService]
      when(svc.createContent("domain", "/", form, user))
        .thenReturn(Future.successful(created("path")))

      val req = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(uri = "/api/v1/contents/domain/", method = HttpMethods.POST, entity = e)
        )
        .futureValue

      req ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.Created
          header("Location") shouldBe Some(Location("path"))
        }
    }

    "return with a 400 if the form data is invalid" in {
      val form =
        CreateForm("title", "path", Folder, "description", "text", "theme", "icon", None, None)

      val user = User("user", Set(Permissions.Contents.Create), Some(CorrectJWT))
      when(defaultExtractor.extractUser(CorrectJWT, Some("domain")))
        .thenReturn(Future.successful(Some(user)))

      val svc = mock[ContentService]
      val errs = List(ValidationError("parent", "is not folder", "parent.folder"))
      when(svc.createContent("domain", "/", form, user))
        .thenReturn(Future.successful(invalid(errs)))

      val req = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(uri = "/api/v1/contents/domain/", method = HttpMethods.POST, entity = e)
        )
        .futureValue

      req ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[ValidationError]] shouldBe errs
        }
    }
  }

  "The PUT path" should {
    "return with a No Content response when anything has been updated" in {
      val form = updateForm()
      val user = User("user", Set(Permissions.Contents.Create), Some(CorrectJWT))
      when(defaultExtractor.extractUser(CorrectJWT, Some("domain")))
        .thenReturn(Future.successful(Some(user)))

      val svc = mock[ContentService]
      when(svc.editContent("domain", "/one", form, user)).thenReturn(Future.successful(noOutput))

      val req = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(uri = "/api/v1/contents/domain/one", method = HttpMethods.PUT, entity = e)
        )
        .futureValue

      req ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.NoContent
        }
    }

    "return with a 400 if the form data is invalid" in {
      val form = updateForm()
      val user = User("user", Set(Permissions.Contents.Create), Some(CorrectJWT))
      when(defaultExtractor.extractUser(CorrectJWT, Some("domain")))
        .thenReturn(Future.successful(Some(user)))

      val svc = mock[ContentService]
      val errs = List(ValidationError("parent", "is not folder", "parent.folder"))
      when(svc.editContent("domain", "/one", form, user))
        .thenReturn(Future.successful(invalid(errs)))

      val req = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(uri = "/api/v1/contents/domain/one", method = HttpMethods.PUT, entity = e)
        )
        .futureValue

      req ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.BadRequest
          entityAs[List[ValidationError]] shouldBe errs
        }
    }
  }

  "The DELETE path" should {
    "return with a No Content response when anything has been deleted" in {
      val request = HttpRequest(uri = "/api/v1/contents/domain/one", method = HttpMethods.DELETE)

      val user = User("user", Set(Permissions.Contents.Delete), Some(CorrectJWT))
      when(defaultExtractor.extractUser(CorrectJWT, Some("domain")))
        .thenReturn(Future.successful(Some(user)))

      val svc = mock[ContentService]
      when(svc.deleteContent("domain", "/one", user)).thenReturn(Future.successful(noOutput))

      request ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.NoContent
        }
    }

    "return a Method Not Allowed if trying to delete the root" in {
      val request = HttpRequest(uri = "/api/v1/contents/domain/", method = HttpMethods.DELETE)

      val user = User("user", Set(Permissions.Contents.Delete), Some(CorrectJWT))
      when(defaultExtractor.extractUser(CorrectJWT, Some("domain")))
        .thenReturn(Future.successful(Some(user)))

      val svc = mock[ContentService]

      request ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new ContentRoutes(svc, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.MethodNotAllowed
          entityAs[RestError] shouldBe RestError("Cannot delete website root")
        }
    }
  }

  def updateForm(): UpdateForm = UpdateForm(
    Some("title"),
    Some("description"),
    Some("text"),
    Some("theme"),
    Some("icon"),
    None,
    Some(Published)
  )

}
