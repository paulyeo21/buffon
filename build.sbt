name := "shoe-dawg-backend"
version := "1.0"
scalaVersion := "2.12.5"

libraryDependencies ++= {
  val akkaV = "2.5.11"
  val akkaHttpV = "10.1.0"
  val scalaTestV = "3.0.4"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "org.scalatest" %% "scalatest" % scalaTestV % Test
  )
}
