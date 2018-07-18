name := "buffon"
version := "1.0"
scalaVersion := "2.12.5"
//dockerBaseImage := "openjdk:jre-alpine"

libraryDependencies ++= {
  val akkaV = "2.5.11"
  val akkaHttpV = "10.1.0"
  val scalaTestV = "3.0.4"
  val elastic4sV = "6.2.6"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
    "com.softwaremill.akka-http-session" %% "core" % "0.5.5",
    "com.typesafe" % "config" % "1.3.2",
    "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.19",
    "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sV,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sV,
    "com.sksamuel.elastic4s" %% "elastic4s-spray-json" % elastic4sV,
    "com.github.t3hnar" %% "scala-bcrypt" % "3.1",
    "org.scalatest" %% "scalatest" % scalaTestV % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test
  )
}

// https://github.com/sbt/sbt-assembly
test in assembly := {}
assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

//enablePlugins(JavaAppPackaging)
//enablePlugins(DockerPlugin)
//enablePlugins(AshScriptPlugin)
