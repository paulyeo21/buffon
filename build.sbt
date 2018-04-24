name := "shoe-dawg-backend"
version := "1.0"
scalaVersion := "2.12.5"
dockerBaseImage := "openjdk:jre-alpine"

libraryDependencies ++= {
  val akkaV = "2.5.11"
  val akkaHttpV = "10.1.0"
  val scalaTestV = "3.0.4"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV exclude("com.typesafe.akka", "akka-http"),
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
    "com.softwaremill.akka-http-session" %% "core" % "0.5.3" exclude("com.typesafe.akka", "akka-http"),
    "com.typesafe" % "config" % "1.3.2",
    "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.18",
    "com.github.t3hnar" %% "scala-bcrypt" % "3.1",
    "org.scalatest" %% "scalatest" % scalaTestV % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test
//    "org.mockito" % "mockito-all" % "1.8.4"
  )
}


enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)
