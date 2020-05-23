package it.ldsoftware.webfleet.driver.security

// $COVERAGE-OFF$ constants don't need testing
object Permissions {

  object Contents {
    val Create = "content.create"
    val Publish = "content.publish"
    val Review = "content.review"
  }

  val all: Set[String] = Set(Contents.Create, Contents.Publish, Contents.Review)

}
// $COVERAGE-ON$
