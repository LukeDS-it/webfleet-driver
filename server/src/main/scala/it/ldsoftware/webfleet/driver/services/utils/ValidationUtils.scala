package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model._

import scala.util.{Failure, Success, Try}

trait ValidationUtils {

  def validate(validator: => Set[FieldError])(execution: => DriverResult): DriverResult = Try {
    val errs = validator
    if (errs.isEmpty) execution
    else ValidationError(errs)
  } match {
    case Success(value) => value
    case Failure(exception) => ServerError(exception.getMessage)
  }

  def ifFound(finder: => Boolean)(execution: => DriverResult): DriverResult = Try {
    if (finder) execution else NotFoundError
  } match {
    case Success(value) => value
    case Failure(exception) => ServerError(exception.getMessage)
  }

  def findAndValidate(finder: => Boolean, validator: => Set[FieldError])(execution: => DriverResult): DriverResult =
    ifFound(finder) {
      validate(validator) {
        execution
      }
    }
}