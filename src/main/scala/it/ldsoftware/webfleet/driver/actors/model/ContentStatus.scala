package it.ldsoftware.webfleet.driver.actors.model

sealed trait ContentStatus

case object Stub extends ContentStatus

case object Review extends ContentStatus

case object Rejected extends ContentStatus

case object Published extends ContentStatus
