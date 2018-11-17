package it.ldsoftware.webfleet.api.v1

package object model {
  sealed trait DriverResult

  sealed trait DriverError extends DriverResult

  case object NotFoundError extends DriverError
  case object ForbiddenError extends DriverError

  case class ServerError(error: String) extends DriverError
  case class ValidationError(errors: List[FieldError]) extends DriverError

  sealed trait DriverSuccess extends DriverResult

  case class Created(id: String) extends DriverSuccess
  case object NoContent extends DriverSuccess


  case class FieldError(field: String, error: String)
}
