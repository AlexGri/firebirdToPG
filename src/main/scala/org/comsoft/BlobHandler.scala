package org.comsoft

import akka.actor.{Actor, ActorLogging}
import org.comsoft.Protocol.{BlobsCopied, CopyBlob}
import scalikejdbc._

/**
 * Created by alexgri on 04.02.15.
 */
class BlobHandler extends Actor with ActorLogging {
  override def receive: Receive = {
    case CopyBlob(name, blobFields, selectPart) =>
      val query = blobFields.mkString(s"$selectPart ID, ", ", ", s" from $name")
      val updateQuery = blobFields.map(_ + " = ?").mkString(s"UPDATE $name SET ", ", ", " where ID = ?")
      val params = DB readOnly { implicit session =>
        session.fetchSize(20000)
        SQL(query).map(rs => extractParams(rs, updateQuery)).list().apply()
      }
      NamedDB('pg) withinTx { implicit session =>
        SQL(updateQuery).batch(params).apply()
      }
      sender() ! BlobsCopied(name)
  }

  def extractParams(rs: WrappedResultSet, query:String) = (2 to rs.metaData.getColumnCount).map(rs.blob) :+ rs.long(1)

}
