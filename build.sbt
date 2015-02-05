import Dependencies._

name := "firebirdToPG"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq (
  akka.actor,
  scalike.jdbc,
  scalike.config,
  firebird,
  pg,
  csvScala,
  slf4j.logback,
  akka.testkit % "test",
  scalatest % "test",
  scalike.testkit % "test"
)
    