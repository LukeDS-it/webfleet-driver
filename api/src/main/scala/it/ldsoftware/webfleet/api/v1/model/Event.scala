package it.ldsoftware.webfleet.api.v1.model

import java.time.LocalDateTime

case class Event(content: Option[Content],
                 start: Option[LocalDateTime],
                 end: Option[LocalDateTime],
                 locName: Option[String],
                 coords: Option[(Double, Double)])
