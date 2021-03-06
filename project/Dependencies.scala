import sbt._

object Dependencies {

  object akka {
    val version = "2.3.9"
    // Core Akka
    val actor                 = "com.typesafe.akka"      %% "akka-actor"                    % version
    val cluster               = "com.typesafe.akka"      %% "akka-cluster"                  % version
    val contrib               = "com.typesafe.akka"      %% "akka-contrib"                  % version intransitive()
    val persistence           = "com.typesafe.akka"      %% "akka-persistence-experimental" % version intransitive()
    val persistence_cassandra = "com.github.krasserm"    %% "akka-persistence-cassandra"    % "0.3.4" intransitive()

    val leveldb               = "org.iq80.leveldb"        % "leveldb"                       % "0.7"
    
    val testkit               = "com.typesafe.akka"      %% "akka-testkit"                  % version
    val tck                   = "com.typesafe.akka"      %% "akka-persistence-tck-experimental" % version
  }

  object scalike {
    val version = "2.2.4"

    val jdbc = "org.scalikejdbc" %% "scalikejdbc" % version
    val testkit = "org.scalikejdbc" %% "scalikejdbc-test" % version
    val config = "org.scalikejdbc" %% "scalikejdbc-config"  % version
    val macros = "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % version
  }

  object slf4j {
    val logback = "ch.qos.logback"  %  "logback-classic"   % "1.1.2"
    val slf4j_simple = "org.slf4j" % "slf4j-simple" % "1.6.1"
  }

  val firebird8 = "org.firebirdsql.jdbc" % "jaybird-jdk18" % "2.2.7"
  val firebird6 = "org.firebirdsql.jdbc" % "jaybird-jdk16" % "2.2.7"

  val pg = "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"

  val dbcp2 = "org.apache.commons" % "commons-dbcp2" % "2.0.+"

  val csvScala = "com.github.tototoshi" %% "scala-csv" % "1.1.2"

  val io = "commons-io" % "commons-io" % "2.4"

  val scalatest    = "org.scalatest"    %% "scalatest"    % "2.2.1"
}
