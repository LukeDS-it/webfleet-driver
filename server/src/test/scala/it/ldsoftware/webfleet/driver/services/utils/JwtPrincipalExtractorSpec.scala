package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.driver.services.utils.AuthenticationUtils.{RoleAddAggregate, RoleDeleteAggregate, RoleEditAggregate, RoleMoveAggregate}
import org.scalatest.{Matchers, WordSpec}

class JwtPrincipalExtractorSpec extends WordSpec with Matchers {
  private val allPerms = Set(RoleAddAggregate, RoleDeleteAggregate, RoleEditAggregate, RoleMoveAggregate)

  "The principal extraction function" should {
    "Correctly extract principal" in {
      val p = new JwtPrincipalExtractor().extractPrincipal(TestUtils.ValidJwt).get
      p.name shouldBe "name"
      p.permissions shouldBe allPerms
    }
  }
}
