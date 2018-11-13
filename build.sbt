val scalatestVersion = "3.0.4"


lazy val api = project in file("api")

lazy val server = (project in file("server"))
  .dependsOn(api)
  .settings(
    name := "jekyll-driver",
    scalaVersion := "2.12.7",
    version := "0.1",
    mainClass in Compile := Some("it.ldsoftware.jekyll.JekyllDriver"),

    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-http" % "18.10.0",
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    )
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(api)

lazy val `jekyll-driver` = (project in file("."))
  .aggregate(api, server)
  .settings(
    run := {
      (run in server in Compile).evaluated
    }
  )
