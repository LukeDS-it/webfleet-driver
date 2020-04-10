package it.ldsoftware.webfleet.driver.service.model

sealed trait ServiceFailure

case class NotFound(searched: String) extends ServiceFailure
case class Invalid(errors: List[String]) extends ServiceFailure
case class UnexpectedError(th: Throwable, message: String) extends ServiceFailure
