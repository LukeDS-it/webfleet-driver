package it.ldsoftware.webfleet.driver.actors.model

import java.time.ZonedDateTime

case class WebCalendar(
    start: ZonedDateTime,
    end: ZonedDateTime,
    locationString: String,
    coords: (Double, Double)
)
