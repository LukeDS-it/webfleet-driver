val scalatestVersion = "3.0.4"

val akkaVersion = "2.5.18"
val akkaHttpVersion = "10.1.5"

val mockitoVersion = "2.23.0"

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
    mainClass in Compile := Some("it.ldsoftware.webfleet.driver.WebfleetDriver"),

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "io.jsonwebtoken" % "jjwt-api" % "0.10.5",
      "io.jsonwebtoken" % "jjwt-impl" % "0.10.5",
      "io.jsonwebtoken" % "jjwt-jackson" % "0.10.5",

      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
      "org.mockito" % "mockito-core" % mockitoVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
    ),

    publish / skip := true
  )

lazy val `webfleet-driver` = (project in file("."))
  .aggregate(api, server)
  .settings(CommonSettings.settings)
  .settings(ReleaseSettings.settings)
  .settings(
    publish / skip := true,
    run := {
      (run in server in Compile).evaluated
    }
  )
