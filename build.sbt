import Dependencies._
import com.github.retronym.SbtOneJar._

name := "firebirdToPG"

version := "1.0"

scalaVersion := "2.11.5"

oneJarSettings

javacOptions ++= Seq("-target", "1.6")

libraryDependencies ++= Seq (
  akka.actor,
  scalike.jdbc,
  scalike.config,
  firebird6,
  pg,
  dbcp2,
  slf4j.logback,
  io,
  akka.testkit % "test",
  scalatest % "test",
  scalike.testkit % "test"
)
    