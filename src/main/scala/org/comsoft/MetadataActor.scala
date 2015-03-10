package org.comsoft

import akka.actor.{Props, Actor, ActorLogging}
import org.comsoft.Protocol.{IndexesCreated, PostMigrateDDL, ExtractMetadata}
import scalikejdbc._

/**
 * Created by alexgri on 05.03.15.
 */
class MetadataActor extends Actor with ActorLogging with FBTiming with PGTiming {

  def sequenceValueSql(name:String):String = "SELECT GEN_ID(" + name + ", 0 ) FROM RDB$DATABASE;"
  def sequenceValue(s:SequenceDefinition) = SQL(sequenceValueSql(s.name)).map(_.int(1)).single()

  override def receive: Receive = {
    case ExtractMetadata =>
      log.debug("extracting metadata from database")
      val parser = SqlParser.apply
      val ParseResult(s, t, i, c) = parser.traverse
      log.debug(
        s"""metadata extracted and converted
           |sequences:    ${s.size}
           |tables:       ${t.size}
           |indexes:      ${i.size}
           |constraints:  ${c.size}
         """.stripMargin)

      val newSequences = fbTiming {
        DB readOnly { implicit session =>
          s.map {
            case sd@SequenceDefinition(name, sql) =>
              val sequenceStart = sequenceValue(sd).apply().getOrElse(0)
              val endPart = s" start ${sequenceStart + 1};"
              SequenceDefinition(name, sql.replaceFirst(";", endPart))
          }
        }
      }

      log.debug("got actual sequences values")

      val ddl = newSequences.map(_.sql).mkString("\n") + t.map(_.sql).mkString("\n")

      pgTiming {
        NamedDB('pg) localTx { implicit session =>
          SQL(ddl).execute().apply()
        }
      }
      log.debug("sequences and tables created")

      sender() ! PostMigrateDDL(i, c)
    case PostMigrateDDL(i, c) =>
      val ddl = i.map(_.sql).mkString("\n") + c.map(_.sql).mkString("\n")
      pgTiming {
        NamedDB('pg) localTx { implicit session =>
          SQL(ddl).execute().apply()
        }
      }
      sender() ! IndexesCreated
  }
}

object MetadataActor {
  def props = Props[MetadataActor]
}
