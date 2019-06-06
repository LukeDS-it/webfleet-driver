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
      "org.postgresql" % "postgresql" % "42.2.5",
      "org.scalikejdbc" %% "scalikejdbc" % "3.3.1",
      "com.auth0" % "jwks-rsa" % "0.8.2",
      "com.auth0" % "java-jwt" % "3.8.1",
      "org.apache.kafka" %% "kafka" % "2.1.0",

      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
      "org.mockito" % "mockito-core" % mockitoVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
      "com.h2database" % "h2" % "1.4.197" % Test,
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
