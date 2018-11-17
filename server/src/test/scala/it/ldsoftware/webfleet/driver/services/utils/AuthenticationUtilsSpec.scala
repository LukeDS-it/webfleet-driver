package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model.{ForbiddenError, NoContent}
import it.ldsoftware.webfleet.driver.services.utils.AuthenticationUtils._
import org.scalatest.{Matchers, WordSpec}

class AuthenticationUtilsSpec extends WordSpec with Matchers with AuthenticationUtils {

  val jwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJuYW1lIiwid2ViZmxlZXQtZHJpdmVyLXJvbGVzIjpbIkFERF9BR0dSRUdBVEUiLCJFRElUX0FHR1JFR0FURSIsIk1PVkVfQUdHUkVHQVRFIiwiREVMRVRFX0FHR1JFR0FURSJdfQ.tMFopWhHG6TR5-1d7nDZ2lCJhCJIAnD1SQrZcldjBB3HfNjHJ3xvbnuqH1jonzLDuE9vpksov1ybrx4OQfMCxDeXTEmRqSJnkKJdvidEoMdQcEhY8phrqpIZNuU9G0BT-snYmvs0tUKHkRTpKfGS4RuhEpsiagxXxwR6HOglV6agjSyk8GNzDJqKL5RFrG4DD3Uq1Wh1ai7kabgExoIAzNzjpgGfY_hdjYU916153TTVmdejBc98iXEKptxJskihHmC7jUKw6lxCIYxad7tSzfEWlWHvEn1t2la5N1eK-yjDMxqTg14NYXnBpSvFvBZ3pY-ZEncP89YVeIdTY0kGJQ"
  val allPerms = Set(RoleAddAggregate, RoleDeleteAggregate, RoleEditAggregate, RoleMoveAggregate)
  val expected = Principal("name", allPerms)

  "The principal extraction function" should {
    "Correctly extract principal" in {
      val p = extractPrincipal(jwt).get
      p.name shouldBe "name"
      p.permissions shouldBe allPerms
    }
  }

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
      authorize(jwt, RoleAddAggregate) { _ =>
        NoContent
      } shouldBe NoContent
    }

    "Not execute code if the principal can not access the function" in {
      authorize(jwt, "UnknownRole") { _ =>
        NoContent
      } shouldBe ForbiddenError
    }
  }

}
