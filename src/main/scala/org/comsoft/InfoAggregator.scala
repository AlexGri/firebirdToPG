package org.comsoft

import java.nio.file.FileSystems

import akka.actor.{ActorLogging, Props, Actor}
import org.comsoft.InfoAggregator.FieldInfo
import org.comsoft.Protocol._
import scalikejdbc._

/**
 * Created by alexgri on 02.02.15.
 */
class InfoAggregator extends Actor with ActorLogging with FBTiming {
  val batchSize = cfg.getInt("batchsize")

  def cfg = context.system.settings.config

  val fieldsQuery = """select rf.rdb$field_name, f.RDB$FIELD_TYPE, f.RDB$FIELD_SUB_TYPE
                      |from rdb$relation_fields rf
                      |join RDB$FIELDS f on (f.RDB$FIELD_NAME = RF.RDB$FIELD_SOURCE)
                      |where rf.rdb$relation_name = """.stripMargin

  override def receive: Receive = {
    case Process(table) =>
      val countQuery = s"select COUNT(1) from $table"

      log.debug(countQuery)
      val count = fbTiming {
        DB readOnly { implicit session =>
          SQL(countQuery).map(rs => rs.int(1)).single().apply().getOrElse(0)
        }
      }

      if (count <= 0) {
        sender() ! WorkDone(table)
      } else {

        val fields:List[FieldInfo] = fbTiming {
          DB readOnly { implicit session =>
            SQL(s"$fieldsQuery '$table'").map(rs => FieldInfo(rs.string(1).trim.toLowerCase, rs.int(2), rs.int(3))).list().apply()
          }
        }
        val selectPart = fields.map{
          //case FieldInfo(name, 7, 0) => s"case $name when 1 then trim('TRUE') when 0 then 'FALSE' end as $name"
          case FieldInfo(name, _, _) => name
        }.mkString("", ", ", s" from $table")

        log.info(s"creating insert query for $table with ${fields.size} fields")

        val questionMarks = (1 to fields.size).map(_ => "?")
        val substitution = questionMarks.mkString("(", ", ", ")")


        val insertQuery = fields.map(_.name).mkString(s"INSERT INTO $table (", ", ", s") VALUES$substitution")

        val numOfBatches = count / batchSize
        val batchInfos = (0 to numOfBatches).map { offset =>
          val s = s"select first $batchSize skip ${offset * batchSize}"
          BatchInfo(s, s"$s $selectPart", generatePath(table, offset), insertQuery)
        }
        sender() ! TableInfo(table,batchInfos)
      }

  }

  def generatePath(tableName:String, partNum:Int) = FileSystems.getDefault()
    .getPath("target", s"$tableName-$partNum.csv")
}

object InfoAggregator {
  def props = Props[InfoAggregator]
  case class FieldInfo(name:String, tpe:Int, subTpe:Int)
}
