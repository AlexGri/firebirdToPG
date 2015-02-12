package org.comsoft

import java.io.ByteArrayInputStream
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
      //log.info(s"executing $batchNum query for $table")
      pgTiming {
        NamedDB('pg) localTx { implicit session =>
          implicit val lom = getLom

          val lines = fbTiming {
            DB readOnly { implicit session =>
              session.fetchSize(blobBatchSize)
              SQL(query).map(seqAndSave).list().apply()
            }
          }

          val q = SQL(insertQuery).batch(lines: _*).apply()

          //lines.foreach(line => insertLine(q, line))
        }
      }
      replyTo ! WorkDone(table)
    }
  }

  def fetchFB(query:String, size:Int) = fbTiming {
    DB readOnly { implicit session =>
      session.fetchSize(batchsize)
      SQL(query).map(toSeq).list().apply()
    }
  }

  def getLom(implicit session:DBSession) =
    session.connection.unwrap(classOf[PGConnection]).getLargeObjectAPI

  def insertLine(q:SQL[Nothing, NoExtractor], fields:Seq[Any])(implicit lom:LargeObjectManager, session:DBSession) = {
    val params = fields.map {
      case BlobBytes(bytes) => saveBlob(bytes)
      case c => c
    }
    q.bind(params:_*).update().apply()
  }

  def toSeq(rs: WrappedResultSet): Seq[Any] = {
    (1 to rs.metaData.getColumnCount).map{i =>
      val tpe = rs.metaData.getColumnType(i)
      tpe match {
        case Types.SMALLINT =>
          val value = rs.nullableShort(i)
          if (value == 0) false
          else if (value == 1) true
          else null
        case Types.VARCHAR =>
          rs.stringOpt(i).map(_.replace(nullSymbol, "")).orNull
        case Types.BLOB | Types.LONGVARBINARY =>
          val blob = rs.blob(i)
          if (blob != null) BlobBytes(blob)
          else null
        case _ => rs.any(i)
      }
    }
  }

  def seqAndSave(rs: WrappedResultSet)(implicit lom:LargeObjectManager): Seq[Any] = {
    (1 to rs.metaData.getColumnCount).map{i =>
      val tpe = rs.metaData.getColumnType(i)
      tpe match {
        case Types.SMALLINT =>
          val value = rs.nullableShort(i)
          if (value == 0) false
          else if (value == 1) true
          else null
        case Types.VARCHAR =>
          rs.stringOpt(i).map(_.replace(nullSymbol, "")).orNull
        case Types.BLOB | Types.LONGVARBINARY =>
          val blob = rs.blob(i)
          if (blob != null) saveBlob(blob)
          else null
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

  def saveBlob(blob:Array[Byte])(implicit lom:LargeObjectManager) = {
    val oid = lom.createLO()
    val lo = lom.open(oid, LargeObjectManager.WRITE)
    val is = new ByteArrayInputStream(blob)
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