import sbt.Keys._
import sbt._
import sbt.librarymanagement.LibraryManagementSyntax

object CommonSettings extends LibraryManagementSyntax {
  lazy val settings = Seq(
    scalaVersion := "2.12.7",
    version := (version in ThisBuild).value
  )
}
