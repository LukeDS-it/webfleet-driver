package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

trait TimeServer {
  def getTime: ZonedDateTime
}

// $COVERAGE-OFF$
object DefaultTimeServer extends TimeServer {
  override def getTime: ZonedDateTime = ZonedDateTime.now()
}
// $COVERAGE-ON$
