import bintray.BintrayKeys._
import sbt.Keys._
import sbt.ThisBuild
import sbt.librarymanagement.LibraryManagementSyntax
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport._

object ReleaseSettings extends LibraryManagementSyntax {
  lazy val settings = Seq(
    bintrayOmitLicense := true,
    releaseProcess := Seq[ReleaseStep](
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("publish"),
      setNextVersion,
      commitNextVersion
    ),
    releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]"
  )
}