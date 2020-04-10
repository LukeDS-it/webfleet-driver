package it.ldsoftware.webfleet.driver.service

package object model {
  type ServiceResult[T] = Either[ServiceFailure, ServiceSuccess[T]]

  def success[T](result: T): ServiceResult[T] = Right(Success(result))
  def created(path: String): ServiceResult[Nothing] = Right(Created(path))
  def noOutput: ServiceResult[Nothing] = Right(NoOutput)

  def notFound[T](searched: String): ServiceResult[T] = Left(NotFound(searched))
  def invalid[T](errors: List[String]): ServiceResult[T] = Left(Invalid(errors))
  def unexpectedError[T](th: Throwable, message: String): ServiceResult[T] =
    Left(UnexpectedError(th, message))
}
