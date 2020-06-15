package it.ldsoftware.webfleet.driver.actors.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContentChildSpec extends AnyWordSpec with Matchers {

  "The getParent function" should {

    "return no parent for the root of a domain" in {
      val child = ContentChild("my-domain/", "title", "description", Page)
      child.getParentPath shouldBe None
    }

    "return the correct path for a child's parent" in {
      val child = ContentChild("my-domain/path/to/child", "title", "description", Page)
      child.getParentPath shouldBe Some("my-domain/path/to")
    }

    "return the correct path for the root's child" in {
      val child = ContentChild("my-domain/path", "title", "description", Page)
      child.getParentPath shouldBe Some("my-domain/")
    }

  }

}
