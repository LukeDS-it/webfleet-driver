package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model.{ForbiddenError, NoContent}
import it.ldsoftware.webfleet.driver.services.utils.AuthenticationUtils._
import org.scalatest.{Matchers, WordSpec}

class AuthenticationUtilsSpec extends WordSpec with Matchers with AuthenticationUtils {

  private val allPerms = Set(RoleAddAggregate, RoleDeleteAggregate, RoleEditAggregate, RoleMoveAggregate)

  override def extractor: PrincipalExtractor = new JwtPrincipalExtractor()

  "The check permission function" should {
    "Return the principal if it contains the permission" in {
      val pr = Principal("name", allPerms)
      checkPermissions(pr, Seq(RoleAddAggregate)) shouldBe Some(pr)
    }

    "Return nothing if it doesn't contain the permission" in {
      val pr = Principal("name", Set(RoleDeleteAggregate))
      checkPermissions(pr, Seq(RoleAddAggregate)) shouldBe None
    }
  }

  "The authorize function" should {
    "Execute code if the principal can access the function" in {
      authorize(TestUtils.ValidJwt, RoleAddAggregate) { _ =>
        NoContent
      } shouldBe NoContent
    }

    "Not execute code if the principal can not access the function" in {
      authorize(TestUtils.ValidJwt, "UnknownRole") { _ =>
        NoContent
      } shouldBe ForbiddenError
    }
  }

}
