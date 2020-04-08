package it.ldsoftware.webfleet.driver.service

package object model {
  type ServiceResult[T] = Either[ServiceFailure, ServiceSuccess[T]]

  sealed trait ServiceSuccess[T]

  sealed trait ServiceFailure

  case class Success[T](result: T) extends ServiceSuccess[T]
  case class Created(path: String) extends ServiceSuccess[Nothing]
  case object NoOutput extends ServiceSuccess[Nothing]

  case class NotFound(searched: String) extends ServiceFailure
  case class Invalid(errors: List[String]) extends ServiceFailure
  case class UnexpectedError(th: Throwable, message: String) extends ServiceFailure

  def success[T](result: T): ServiceResult[T] = Right(Success(result))
  def created(path: String): ServiceResult[Nothing] = Right(Created(path))
  def noOutput: ServiceResult[Nothing] = Right(NoOutput)

  def notFound[T](searched: String): ServiceResult[T] = Left(NotFound(searched))
  def invalid[T](errors: List[String]): ServiceResult[T] = Left(Invalid(errors))
  def unexpectedError[T](th: Throwable, message: String): ServiceResult[T] = Left(UnexpectedError(th, message))
}
