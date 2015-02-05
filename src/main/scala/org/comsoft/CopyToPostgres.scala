package org.comsoft

import akka.actor.{Props, Actor}
import org.comsoft.Protocol.{Copied, CopyTo}
import scalikejdbc._

/**
 * Created by alexgri on 03.02.15.
 */
class CopyToPostgres extends Actor {

  override def receive: Receive = {
    case CopyTo(filename, table, columns) =>
      val format = com.github.tototoshi.csv.defaultCSVFormat
      val query = columns.mkString(s"copy $table (", ", ",
        s""") FROM '$filename'
           |WITH (FORMAT CSV, DELIMITER '${format.delimiter}',
           |HEADER false, QUOTE '${format.quoteChar}',
           |ESCAPE '${format.escapeChar}')""".stripMargin)

       NamedDB('pg) localTx { implicit session =>
          SQL(query).execute().apply()
       }
      sender() ! Copied(filename)
  }
}

object CopyToPostgres {
  def props = Props[CopyToPostgres]
}