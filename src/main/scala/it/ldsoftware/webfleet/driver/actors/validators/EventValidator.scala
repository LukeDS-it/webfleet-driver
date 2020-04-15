package it.ldsoftware.webfleet.driver.actors.validators

import it.ldsoftware.webfleet.driver.actors.model.{ValidationError, WebCalendar}

class EventValidator {
  def validate(event: WebCalendar): Option[ValidationError] =
    if (event.start.isAfter(event.end))
      Some(ValidationError("event.start", "Start date cannot be after end date", "date.future"))
    else
      None
}
