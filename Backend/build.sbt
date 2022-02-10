version := "0.1"

scalaVersion := "2.13.8"

lazy val darcy = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    dockerBaseImage := "openjdk:8-jre-slim",
    dockerExposedPorts := Seq(8080),
    name := "Darcy",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.7",
      "org.http4s" %% "http4s-dsl" % "0.23.0-RC1",
      "org.http4s" %% "http4s-blaze-server" % "0.23.0-RC1",
      "org.http4s" %% "http4s-blaze-client" % "0.23.0-RC1",
      "org.typelevel" %% "cats-effect" % "3.3.5",
      "co.fs2" %% "fs2-core" % "3.0.3",
      "co.fs2" %% "fs2-io" % "3.0.3",
      "org.log4s" %% "log4s" % "1.8.2",
      "com.google.protobuf" % "protobuf-java" % "3.19.1",
      "com.google.protobuf" % "protobuf-java-util" % "3.19.1",
      "org.http4s" %% "http4s-circe" % "0.23.0-RC1",
      "io.circe" %% "circe-generic" % "0.14.1"
    ),
    mainClass := Some("ru.intfox.darcy.Main")
  )
