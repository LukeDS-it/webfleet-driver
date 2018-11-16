package it.ldsoftware.webfleet.api.v1.model

sealed trait ContentStatus

case object Stub extends ContentStatus
case object Published extends ContentStatus
case object WaitingReview extends ContentStatus

case class Rejected(motivation: String) extends ContentStatus
