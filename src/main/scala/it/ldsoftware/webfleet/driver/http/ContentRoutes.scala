package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.http.{RestError, RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.commons.service.model.NoResult
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, UpdateForm, WebContent}
import it.ldsoftware.webfleet.driver.security.Permissions
import it.ldsoftware.webfleet.driver.service.ContentService

class ContentRoutes(contentService: ContentService, val extractor: UserExtractor)
    extends RouteHelper {

  def routes: Route = path("api" / "v1" / "contents" / Segment / Remaining) { (domain, rest) =>
    val remaining = s"/$rest"
    getContents(domain, remaining) ~
      createContent(domain, remaining) ~
      editContent(domain, remaining) ~
      deleteContent(domain, remaining)
  }

  private def getContents(domain: String, remaining: String): Route =
    get {
      authorize(domain, Permissions.Contents.Read) { _ =>
        svcCall[WebContent](contentService.getContent(domain, remaining))
      }
    }

  private def createContent(domain: String, remaining: String): Route =
    post {
      authorize(domain, Permissions.Contents.Create) { user =>
        entity(as[CreateForm]) { form =>
          svcCall[String](contentService.createContent(domain, remaining, form, user))
        }
      }
    }

  private def editContent(domain: String, remaining: String): Route =
    put {
      authorize(domain, Permissions.Contents.Create) { user =>
        entity(as[UpdateForm]) { form =>
          svcCall[NoResult](contentService.editContent(domain, remaining, form, user))
        }
      }
    }

  private def deleteContent(domain: String, remaining: String): Route =
    delete {
      authorize(domain, Permissions.Contents.Delete) { user =>
        if (remaining == "/") {
          complete(StatusCodes.MethodNotAllowed -> RestError("Cannot delete website root"))
        } else {
          svcCall[NoResult](contentService.deleteContent(domain, remaining, user))
        }
      }
    }

}
