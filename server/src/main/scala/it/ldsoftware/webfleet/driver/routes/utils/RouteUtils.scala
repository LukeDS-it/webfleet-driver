package it.ldsoftware.webfleet.driver.routes.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive, Directives, PathMatcher, Route}
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.api.v1.auth.Principal
import it.ldsoftware.webfleet.api.v1.model
import it.ldsoftware.webfleet.api.v1.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait RouteUtils extends SprayJsonSupport with DefaultJsonProtocol with LazyLogging with Directives {

  val realm: String = "realm"

  def extractor: PrincipalExtractor

  implicit val FieldErrorFormatter: RootJsonFormat[FieldError] = jsonFormat2(FieldError)

  def getPath[L](x: PathMatcher[L]): Directive[L] = get & path(x)

  def postPath[L](x: PathMatcher[L]): Directive[L] = post & path(x)

  def putPath[L](x: PathMatcher[L]): Directive[L] = put & path(x)

  def deletePath[L](x: PathMatcher[L]): Directive[L] = delete & path(x)

  def authenticator(credentials: Credentials): Option[Principal] = credentials match {
    case Credentials.Missing => None
    case Credentials.Provided(identifier) => extractor extractPrincipal identifier
  }

  def completeFrom(processing: => DriverResult): Route = onComplete(Future(processing)) {
    case Success(result) => result match {
      case error: DriverError => completeFromError(error)
      case success: DriverSuccess => completeFromSuccess(success)
    }
    case Failure(exception) =>
      logger.error("Unexpected exception while processing the call", exception)
      completeFromError(ServerError(exception.getMessage))
  }

  private def completeFromSuccess(success: DriverSuccess): Route = success match {
    case Created(id) => complete(StatusCodes.Created, id)
    case model.NoContent => complete(StatusCodes.NoContent)
  }

  private def completeFromError(error: DriverError): Route = error match {
    case NotFoundError => complete(StatusCodes.NotFound)
    case ForbiddenError => complete(StatusCodes.Forbidden)
    case ServerError(exception) => complete(StatusCodes.InternalServerError -> exception)
    case ValidationError(errorList) => complete(StatusCodes.BadRequest -> errorList)
  }
}
