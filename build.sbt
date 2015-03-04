import Dependencies._
import com.github.retronym.SbtOneJar._

name := "firebirdToPG"

version := "1.0"

scalaVersion := "2.11.5"

oneJarSettings

libraryDependencies ++= Seq (
  akka.actor,
  scalike.jdbc,
  scalike.config,
  firebird,
  pg,
  pool,
  dbcp2,
  csvScala,
  slf4j.logback,
  io,
  akka.testkit % "test",
  scalatest % "test",
  scalike.testkit % "test"
)
    