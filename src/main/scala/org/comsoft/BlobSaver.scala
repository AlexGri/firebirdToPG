/*
package org.comsoft

import java.io.ByteArrayInputStream
import java.sql.{Blob, DriverManager}
import java.util.Properties

import akka.actor.{Props, Actor, ActorLogging}
import org.apache.commons.io.IOUtils
import org.comsoft.Protocol.{BlobProcessed, BlobBytes}
import org.postgresql.jdbc4.Jdbc4Connection
import org.postgresql.largeobject.LargeObjectManager

/**
 * Created by alexgri on 10.02.15.
 */
class BlobSaver extends Actor with ActorLogging with PlainPGConnection {

  def receive = {
    case BlobBytes(oid, bytes) =>
      val lo = lom.open(oid, LargeObjectManager.WRITE)
      val is = new ByteArrayInputStream(bytes)
      val os = lo.getOutputStream
      try {
        IOUtils.copy(is, os)
        connection
        connection.commit()
        sender() ! BlobProcessed
      } catch {
        case e:Exception => connection.rollback(); throw new Exception("")
      } finally {
        IOUtils.closeQuietly(is)
        IOUtils.closeQuietly(os)
      }
  }
}

object BlobSaver {
  def props = Props[BlobSaver]
}

trait PlainPGConnection {
  self:Actor =>
  lazy val cfg = context.system.settings.config

  lazy val url = cfg.getString("db.pg.url")
  lazy val props = new Properties()
  props.setProperty("user", cfg.getString("db.pg.user"))
  props.setProperty("password", cfg.getString("db.pg.password"))
  //props.setProperty("ssl","true")

  lazy val connection = getConnection
  lazy val lom = new LargeObjectManager(connection)

  def getConnection = {
    val c = DriverManager.getConnection(url, props).asInstanceOf[Jdbc4Connection]
    c.setAutoCommit(false)
    c
  }

  @throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    if (connection != null && !connection.isClosed) {
      connection.close()
    }
    context.children foreach { child ⇒
      context.unwatch(child)
      context.stop(child)
    }
    postStop()
  }
}
*/
