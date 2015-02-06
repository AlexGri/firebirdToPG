package org.comsoft

import akka.actor.{Props, Actor, ActorLogging}
import org.comsoft.Protocol.{Collect, DoExport}
import scalikejdbc._

/**
 * Created by alexgri on 02.02.15.
 */
class TableRetriever extends Actor with ActorLogging {

  val q =
    """
      |select rdb$relation_name
      |from rdb$relations
      |where rdb$view_blr is null
      |and (rdb$system_flag is null or rdb$system_flag = 0)
    """.stripMargin

  override def receive: Receive = {
    case Collect =>
      val tableNames = DB readOnly { implicit session =>
        SQL(q).map(rs => rs.string(1).trim).list().apply()
      }

      val truncation = tableNames.map(name => s"TRUNCATE $name").mkString(";\n")
      NamedDB('pg) localTx {implicit session =>
        SQL(truncation).execute().apply()
      }
      sender() ! DoExport(tableNames)
  }
}

object TableRetriever {
  def props = Props[TableRetriever]
}

