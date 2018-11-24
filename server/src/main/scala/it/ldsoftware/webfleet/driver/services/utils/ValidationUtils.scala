package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model.{DriverResult, FieldError, ValidationError}

trait ValidationUtils {
  def validate(validator: => Set[FieldError])(execution: => DriverResult): DriverResult = {
    val errs = validator
    if (errs.isEmpty) execution
    else ValidationError(errs)
  }
}
