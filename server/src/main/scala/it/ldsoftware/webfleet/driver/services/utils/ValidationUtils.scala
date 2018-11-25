package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model.{DriverResult, FieldError, NotFoundError, ValidationError}

trait ValidationUtils {

  def validate(validator: => Set[FieldError])(execution: => DriverResult): DriverResult = {
    val errs = validator
    if (errs.isEmpty) execution
    else ValidationError(errs)
  }

  def ifFound(finder: => Boolean)(execution: => DriverResult): DriverResult = if (finder) execution else NotFoundError

  def findAndValidate(finder: => Boolean, validator: => Set[FieldError])(execution: => DriverResult): DriverResult =
    ifFound(finder) {
      execution
    }
}