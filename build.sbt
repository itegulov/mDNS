name := "mdns"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "com.twitter" %% "util-eval" % "6.41.0"
)
    