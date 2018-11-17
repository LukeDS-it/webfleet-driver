package it.ldsoftware.webfleet.driver.services.utils

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

}
