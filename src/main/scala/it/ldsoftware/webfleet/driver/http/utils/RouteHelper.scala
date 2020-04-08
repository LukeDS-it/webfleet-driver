package it.ldsoftware.webfleet.driver.http.utils

import akka.http.scaladsl.marshalling.{ToEntityMarshaller, ToResponseMarshallable}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.http.model.out.RestError
import it.ldsoftware.webfleet.driver.service.model
import it.ldsoftware.webfleet.driver.service.model._

import scala.concurrent.Future

trait RouteHelper extends LazyLogging with FailFastCirceSupport with Directives {

  def callService[T](serviceCall: => Future[ServiceResult[T]])(implicit marshaller: ToEntityMarshaller[T]): Route = onSuccess(serviceCall) {
    case Right(result) => handleSuccess(result)
    case Left(error)   => handleFailure(error)
  }

  def handleSuccess[T](result: ServiceSuccess[T])(implicit marshaller: ToEntityMarshaller[T]): Route = result match {
    case Success(result) => complete(result)
    case Created(path)   => complete(path)
    case model.NoOutput  => complete(StatusCodes.NoContent)
  }

  def handleFailure(failure: ServiceFailure): Route = failure match {
    case Invalid(errors) => complete(StatusCodes.BadRequest -> errors)
    case NotFound(searched) =>
      complete(StatusCodes.NotFound -> RestError(s"Requested resource $searched could not be found"))
    case UnexpectedError(th, message) =>
      logger.error("An unexpected error has happened", th)
      complete(StatusCodes.InternalServerError -> RestError(message))
  }

  implicit class ToRestOutputMapper[T](value: T) {
    def toRestOutput[R](implicit restMapper: RestMapper[T, R]): R = restMapper.map(value)
  }

}
