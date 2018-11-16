val scalatestVersion = "3.0.4"


lazy val api = (project in file("api"))
  .settings(CommonSettings.settings)
  .settings(ReleaseSettings.settings)
  .settings(
    name := "webfleet-driver-api"
  )

lazy val server = (project in file("server"))
  .dependsOn(api)
  .enablePlugins(JavaAppPackaging)
  .settings(CommonSettings.settings)
  .settings(
    name := "webfleet-driver",
    mainClass in Compile := Some("it.ldsoftware.jekyll.JekyllDriver"),

    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-http" % "18.10.0",
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    ),

    publish / skip := true
  )

lazy val `webfleet-driver` = (project in file("."))
  .aggregate(api, server)
  .settings(CommonSettings.settings)
  .settings(ReleaseSettings.settings)
  .settings(
    test / aggregate := false,
    publish / skip := true,
    run := {
      (run in server in Compile).evaluated
    }
  )
