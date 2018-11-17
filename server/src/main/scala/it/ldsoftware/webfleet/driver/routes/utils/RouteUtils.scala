package it.ldsoftware.webfleet.driver.routes.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.api.v1.model
import it.ldsoftware.webfleet.api.v1.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait RouteUtils extends SprayJsonSupport with DefaultJsonProtocol with LazyLogging with Directives {

  implicit val FieldErrorFormatter: RootJsonFormat[FieldError] = jsonFormat2(FieldError)

  def authenticator(credentials: Credentials): Option[String] = credentials match {
    case Credentials.Missing => None
    case Credentials.Provided(identifier) => Some(identifier)
  }

  def completeFrom(result: DriverResult): Route = result match {
    case error: DriverError => completeFromError(error)
    case success: DriverSuccess => completeFromSuccess(success)
  }

  def completeFromSuccess(success: DriverSuccess): Route = success match {
    case Created(id) => complete(StatusCodes.Created, id)
    case model.NoContent => complete(StatusCodes.NoContent)
  }

  def completeFromError(error: DriverError): Route = error match {
    case NotFoundError => complete(StatusCodes.NotFound)
    case ForbiddenError => complete(StatusCodes.Forbidden)
    case ServerError(exception) => complete(StatusCodes.InternalServerError -> exception)
    case ValidationError(errorList) => complete(StatusCodes.BadRequest -> errorList)
  }
}
