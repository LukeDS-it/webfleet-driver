package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model.NoContent
import org.scalatest.{Matchers, WordSpec}
import AuthenticationUtils._

class AuthenticationUtilsSpec extends WordSpec with Matchers with AuthenticationUtils {

  "The authentication function" should {
    "correctly retrieve the principal" in {
      val jwt = ""
      authorize(jwt) { principal =>
        principal shouldBe Principal("name", List(RoleAddAggregate))
        NoContent
      }
    }
  }

}
