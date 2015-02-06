package org.comsoft

import java.nio.charset.Charset
import java.sql.Types

import akka.actor.{Actor, ActorLogging, Props}
import org.comsoft.Protocol._
import scalikejdbc._

/**
 * Created by alexgri on 02.02.15.
 */
class TableProcessor extends Actor with ActorLogging with FBTiming with PGTiming {

  val charset = Charset.forName("UTF-8")
  val encoder = charset.newEncoder()

  val nullSymbol = "\u0000"

  val batchsize = context.system.settings.config.getInt("batchsize")

  override def receive: Receive = {
    case BatchPart(table, BatchInfo(query, insertQuery)) => {
      val replyTo = sender()
      //log.info(s"executing $batchNum query for $table")
      val lines = fbTiming {
        DB readOnly { implicit session =>
          session.fetchSize(batchsize)
          SQL(query).map(toSeq).list().apply()
        }
      }
      //log.info(s"executing $insertQuery with parameters (only first one) ${lines.head}")
      pgTiming {
        NamedDB('pg) localTx { implicit session =>
          //lines.map(params => SQL(insertQuery).bind(params:_*).update.apply())
          SQL(insertQuery).batch(lines: _*).apply()
        }
      }
      replyTo ! WorkDone(table)

    }

      def toSeq(rs: WrappedResultSet): Seq[Any] = {
        (1 to rs.metaData.getColumnCount).map{i =>
          val tpe = rs.metaData.getColumnType(i)
          if (tpe == Types.SMALLINT) {
            val value = rs.nullableShort(i)
            if (value == 0) false
            else if (value == 1) true
            else null
          } else if (tpe == Types.VARCHAR) {
            rs.stringOpt(i).map(_.replace(nullSymbol, "")).orNull
          }
          else rs.any(i)
        }
      }
  }
}
object TableProcessor {
  def props = Props[TableProcessor]
}