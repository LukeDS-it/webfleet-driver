package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, UpdateForm, WebContent}
import it.ldsoftware.webfleet.driver.http.model.out.RestError
import it.ldsoftware.webfleet.driver.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentService
import it.ldsoftware.webfleet.driver.service.model.NoResult

class ContentRoutes(contentService: ContentService, val extractor: UserExtractor)
    extends RouteHelper {

  def routes: Route = path("api" / "v1" / "contents" / Remaining) { rest =>
    val remaining = s"/$rest"
    login { user =>
      getContents(remaining) ~
        createContent(remaining, user) ~
        editContent(remaining, user) ~
        deleteContent(remaining, user)
    }
  }

  private def getContents(remaining: String): Route = get {
    svcCall[WebContent](contentService.getContent(remaining))
  }

  private def createContent(remaining: String, user: User): Route = post {
    entity(as[CreateForm]) { form =>
      svcCall[String](contentService.createContent(remaining, form, user))
    }
  }

  private def editContent(remaining: String, user: User): Route = put {
    entity(as[UpdateForm]) { form =>
      svcCall[NoResult](contentService.editContent(remaining, form, user))
    }
  }

  private def deleteContent(remaining: String, user: User): Route = delete {
    if (remaining == "/") {
      complete(StatusCodes.MethodNotAllowed -> RestError("Cannot delete website root"))
    } else {
      svcCall[NoResult](contentService.deleteContent(remaining, user))
    }
  }

}
