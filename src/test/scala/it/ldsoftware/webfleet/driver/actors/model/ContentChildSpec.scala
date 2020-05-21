package it.ldsoftware.webfleet.driver.actors.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContentChildSpec extends AnyWordSpec with Matchers {

  "The getParent function" should {
    "return the correct path for a child's parent" in {
      val child = ContentChild("/path/to/child", "title", "description", Page)
      child.getParentPath shouldBe "/path/to"
    }
  }

}
