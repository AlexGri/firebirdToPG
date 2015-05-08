package org.comsoft

import java.sql.{Blob, Types}

import akka.actor.{Actor, ActorLogging, Props}
import org.apache.commons.io.IOUtils
import org.comsoft.Protocol._
import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager
import scalikejdbc._

/**
 * Created by alexgri on 02.02.15.
 */
class TableProcessor extends Actor with ActorLogging with FBTiming with PGTiming {
  val nullSymbol = "\u0000"

  val batchsize = context.system.settings.config.getInt("batchsize")
  val blobBatchSize = context.system.settings.config.getInt("blobBatchSize")

  override def receive: Receive = {
    case BatchPart(table, BatchInfo(query, insertQuery)) => {
      val replyTo = sender()
      //log.info(s"executing $batchNum query for $table")
      val lines = fetchFB(query, batchsize)
      //log.info(s"executing $insertQuery with parameters (only first one) ${lines.head}")
      pgTiming {
        NamedDB('pg) localTx { implicit session =>
          //lines.map(params => SQL(insertQuery).bind(params:_*).update.apply())
          SQL(insertQuery).batch(lines: _*).apply()
        }
      }
      replyTo ! WorkDone(table)
    }
    case BatchPart(table, BlobBatchInfo(query, insertQuery)) => {
      val replyTo = sender()
      log.info(s"executing blob insert query for $table")
      pgTiming {
        NamedDB('pg) localTx { implicit session =>
          implicit val lom = Some(getLom)
          val lines = fetchFB(query, blobBatchSize)
          val q = SQL(insertQuery).batch(lines: _*).apply()

          //lines.foreach(line => insertLine(q, line))
        }
      }
      replyTo ! WorkDone(table)
    }
  }

  def fetchFB(query:String, size:Int)(implicit lom:Option[LargeObjectManager] = None) = fbTiming {
    DB readOnly { implicit session =>
      session.fetchSize(size)
      SQL(query).map(seqAndSave).list().apply()
    }
  }

  def getLom(implicit session:DBSession) =
    session.connection.unwrap(classOf[PGConnection]).getLargeObjectAPI

  def seqAndSave(rs: WrappedResultSet)(implicit lom:Option[LargeObjectManager]): Seq[Any] = {
    (1 to rs.metaData.getColumnCount).map{i =>
      val tpe = rs.metaData.getColumnType(i)
      tpe match {
        case Types.VARCHAR =>
          rs.stringOpt(i).map(_.replace(nullSymbol, "")).orNull
        case Types.SMALLINT => rs.intOpt(i).map(_ == 1).orNull
        /*case Types.BLOB | Types.LONGVARBINARY =>
          val blob = rs.blob(i)
          if (blob != null) saveBlob(blob)(lom.get)
          else null*/
        case _ => rs.any(i)
      }
    }
  }

  def saveBlob(blob:Blob)(implicit lom:LargeObjectManager) = {
    val oid = lom.createLO()
    val lo = lom.open(oid, LargeObjectManager.WRITE)
    val is = blob.getBinaryStream
    val os = lo.getOutputStream
    try {
      IOUtils.copy(is, os)
    } finally {
      IOUtils.closeQuietly(is)
      IOUtils.closeQuietly(os)
    }
    oid
  }

}
object TableProcessor {
  def props = Props[TableProcessor]
}