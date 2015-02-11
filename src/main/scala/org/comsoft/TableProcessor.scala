package org.comsoft

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.sql.{Blob, Types}

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.{BalancingPool, FromConfig}
import akka.pattern._
import akka.util.Timeout
import org.apache.commons.io.IOUtils
import org.comsoft.Protocol._
import org.postgresql.PGConnection
import org.postgresql.jdbc4.Jdbc4Connection
import org.postgresql.largeobject.LargeObjectManager
import scalikejdbc._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by alexgri on 02.02.15.
 */
class TableProcessor extends Actor with ActorLogging with FBTiming with PGTiming /*with PlainPGConnection */{

  val charset = Charset.forName("UTF-8")
  val encoder = charset.newEncoder()

  val nullSymbol = "\u0000"

  val batchsize = context.system.settings.config.getInt("batchsize")
  val blobsaver = context.actorSelection("/main/operator/blobsaver")
  implicit val timeout = Timeout(5 seconds)

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

      val q = SQL(insertQuery)
      val qs = lines.map(line =>
              q.bind(line.map {
                  case BlobBytes(bytes) =>  save(bytes)
                  case c => c
                }:_*
              ).update()
            )
      NamedDB('pg) localTx { implicit session =>
        qs.foreach(_.apply())
      }
        /*NamedDB('pg) localTx { implicit session =>
          val q = SQL(insertQuery)
          val pgconnection = session.connection.unwrap(classOf[PGConnection])
          val lom = pgconnection.getLargeObjectAPI
          lines.map(line =>

            q.bind(line.map {
            case BlobBytes(bytes) => saveBlob(bytes)(lom)
            case c => c
          }:_*).update().apply())
        }
        */


     /* val db = DB(getConnection)
      try {
        db.begin()
        db withinTx  { tx =>
          val lom = new LargeObjectManager(tx.connection.asInstanceOf[Jdbc4Connection])
          lines.map(line => line.map {
            case column if column.isInstanceOf[Blob] =>
              val oid = lom.createLO()
              sender() ! BlobBytes(oid, column.asInstanceOf[Blob])
              oid
            case c => c
          })
        }
        db.commit()
      } catch {
        case e:Exception => db.rollback(); throw e
      } finally {
        db.close()
      }*/

      //log.info(s"executing $insertQuery with parameters (only first one) ${lines.head}")
     /* pgTiming {
        NamedDB('pg) localTx { implicit session =>
          lines.map(params => SQL(insertQuery).bind(params:_*).update.apply())
          //SQL(insertQuery).batch(lines: _*).apply()
        }
      }*/
      replyTo ! WorkDone(table)

    }


  }

  def save(blob:Array[Byte]) = {
    NamedDB('pg) localTx { session =>
      val pgconnection = session.connection.unwrap(classOf[PGConnection])
      val lom = pgconnection.getLargeObjectAPI
      saveBlob(blob)(lom)
    }
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
        case x =>

          //log.info(s"@@@@@@@@@@@ ${rs.metaData.getColumnName(i)} $x")
          rs.any(i)
      }
    }
  }

  def saveBlob(blob:Array[Byte])(lom:LargeObjectManager) = {
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