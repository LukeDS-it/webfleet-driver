package it.ldsoftware.webfleet.driver.actors.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContentChildSpec extends AnyWordSpec with Matchers {

  "The getParent function" should {

    "return the correct path for a child's parent" in {
      val child = ContentChild("/path/to/child", "title", "description", Page)
      child.getParentPath shouldBe Some("/path/to")
    }

    "return the correct path for the root's child" in {
      val child = ContentChild("/path", "title", "description", Page)
      child.getParentPath shouldBe Some("/")
    }

    "return no parent for the root" in {
      val child = ContentChild("/", "title", "description", Page)
      child.getParentPath shouldBe None
    }

  }

}
