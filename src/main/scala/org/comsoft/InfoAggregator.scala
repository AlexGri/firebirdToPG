package org.comsoft

import akka.actor.{Actor, ActorLogging, Props}
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

  val pkFields = """SELECT s.rdb$field_name
                   |FROM rdb$index_segments AS s
                   |LEFT JOIN rdb$relation_constraints AS rc ON (rc.rdb$index_name = s.rdb$index_name)
                   |WHERE rc.rdb$constraint_type = 'PRIMARY KEY'
                   |AND  rc.rdb$relation_name = 'JBPM5_HT_N10N_JBPM5_HT_MAILNHDR'
                   |""".stripMargin

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

        val (fields, orderPart) = fbTiming {
          DB readOnly { implicit session =>
            val order = SQL(pkFields).map(_.string(1).trim.toLowerCase).list().apply() match {
              case l if l.isEmpty => ""
              case l => l.mkString("ORDER BY ", ", ", "")
            }

            val infos = SQL(s"$fieldsQuery '$table'").map(rs => FieldInfo(rs.string(1).trim.toLowerCase, rs.int(2), rs.int(3))).list().apply()
            (infos, order)
          }
        }
        val selectPart = fields.map{
          //case FieldInfo(name, 7, 0) => s"case $name when 1 then trim('TRUE') when 0 then 'FALSE' end as $name"
          case FieldInfo(name, _, _) => name
        }.mkString("", ", ", s" from $table $orderPart")

        log.info(s"creating insert query for $table with ${fields.size} fields")

        val questionMarks = (1 to fields.size).map(_ => "?")
        val substitution = questionMarks.mkString("(", ", ", ")")


        val insertQuery = fields.map(_.name).mkString(s"INSERT INTO $table (", ", ", s") VALUES$substitution")

        val batchInfos = if (orderPart.isEmpty) {
          Seq(BatchInfo(s"select $selectPart", insertQuery))
        } else {
          val numOfBatches = count / batchSize
          (0 to numOfBatches).map { offset =>
            val s = s"select first $batchSize skip ${offset * batchSize}"
            BatchInfo(s"$s $selectPart", insertQuery)
          }
        }
        sender() ! TableInfo(table,batchInfos)
      }

  }
}

object InfoAggregator {
  def props = Props[InfoAggregator]
  case class FieldInfo(name:String, tpe:Int, subTpe:Int)
}
