package it.ldsoftware.webfleet.driver.actors.model

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.commons.service.model.ValidationError
import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class WebCalendar(
    start: ZonedDateTime,
    end: ZonedDateTime,
    locationString: String,
    coords: (Double, Double)
) extends CborSerializable {
  def validationError: Option[ValidationError] =
    if (start.isAfter(end))
      Some(ValidationError("event.start", "Start date cannot be after end date", "date.future"))
    else
      None
}
