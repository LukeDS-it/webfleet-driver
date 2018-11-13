package it.ldsoftware.jekyll.api.v1.model

sealed trait DriverError

case object NotFoundError extends DriverError
case object ForbiddenError extends DriverError

case class ServerError(error: String) extends DriverError
case class ValidationError(errors: List[FieldError]) extends DriverError


case class FieldError(field: String, error: String)
