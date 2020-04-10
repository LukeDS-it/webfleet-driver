package it.ldsoftware.webfleet.driver.service.model

sealed trait ServiceSuccess[T]

case class Success[T](result: T) extends ServiceSuccess[T]
case class Created(path: String) extends ServiceSuccess[Nothing]
case object NoOutput extends ServiceSuccess[Nothing]
