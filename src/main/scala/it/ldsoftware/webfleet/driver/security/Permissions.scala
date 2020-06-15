package it.ldsoftware.webfleet.driver.security

// $COVERAGE-OFF$ constants don't need testing
object Permissions {

  object Contents {
    val Read = "content:read"
    val Create = "content:create"
    val Delete = "content:delete"
    val Publish = "content:publish"
    val Review = "content:review"
  }

  val AllPermissions: Set[String] = Set(
    Contents.Read,
    Contents.Create,
    Contents.Delete,
    Contents.Publish,
    Contents.Review
  )

}
// $COVERAGE-ON$
