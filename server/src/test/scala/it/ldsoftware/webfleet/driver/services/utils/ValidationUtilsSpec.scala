package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model._
import org.scalatest.{Matchers, WordSpec}

class ValidationUtilsSpec extends WordSpec with Matchers with ValidationUtils {

  "The ifFound function" should {
    "Return the result if the find condition is met" in {
      ifFound(finder = true) {
        NoContent
      } shouldBe NoContent
    }

    "Return a not found error if the condition is not met" in {
      ifFound(finder = false) {
        NoContent
      } shouldBe NotFoundError
    }

    "Return a server error if there is an exception during the function evaluation" in {
      ifFound(finder = throw new Exception("Exception")) {
        NoContent
      } shouldBe ServerError("Exception")
    }
  }

  "The validate function" should {
    "Return the result if the validation function returns an empty set of errors" in {
      validate(Set()) {
        NoContent
      } shouldBe NoContent
    }

    "Return a validation error if the validation function returns errors" in {
      val expected = Set(FieldError("field", "value"))
      validate(expected) {
        NoContent
      } shouldBe ValidationError(expected)
    }

    "Return a server error if there is an exception during the function evaluation" in {
      validate(validator = throw new Exception("Exception")) {
        NoContent
      } shouldBe ServerError("Exception")
    }
  }

}
