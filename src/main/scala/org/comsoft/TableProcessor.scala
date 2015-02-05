package org.comsoft

import java.io.StringWriter
import java.nio.CharBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.charset.Charset
import java.nio.file.{FileSystems, StandardOpenOption}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.tototoshi.csv._
import org.comsoft.Protocol.{BatchInfo, BatchPart, WorkDone}
import scalikejdbc._

/**
 * Created by alexgri on 02.02.15.
 */
class TableProcessor extends Actor with ActorLogging {

  val charset = Charset.forName("UTF-8")
  val encoder = charset.newEncoder()

override def receive: Receive = {
    case BatchPart(table,BatchInfo(_, query, saveTo)) => {
      val replyTo = sender()
      //log.info(s"executing $batchNum query for $table")
      val lines = DB readOnly { implicit session =>
        session.fetchSize(20000)
        SQL(query).map(toSeq).list().apply()
      }
      //      log.info(s"execution completed. first line 200 symbols ${lines.head.mkString.take(200)}")
      //log.info(s"writing ${lines.size} lines to file")

      val channel = AsynchronousFileChannel.open(saveTo, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
      val wc = new WriteCompletionHandler(replyTo, WorkDone(table), channel)
      write(lines, wc)
    }
  }

  def toSeq(rs: WrappedResultSet): Seq[String] =  {
    (1 to rs.metaData.getColumnCount).map(rs.string)
  }



  def write(values:Seq[Seq[String]], writeCompletionHandler: WriteCompletionHandler) = {
    val sw = new StringWriter();
    val writer = CSVWriter.open(sw)
    writer.writeAll(values)
    writer.close()

    writeCompletionHandler.channel.write(encoder.encode(CharBuffer.wrap(sw.getBuffer)), 0, null, writeCompletionHandler)
  }

  private[this] class WriteCompletionHandler(val receiver: ActorRef, val cmd: WorkDone, val channel: AsynchronousFileChannel) extends CompletionHandler[Integer, AnyRef] {
    def completed(result: Integer, attachment: AnyRef): Unit = {channel.close;receiver ! cmd}

    def failed(exc: Throwable, attachment: AnyRef): Unit = {channel.close;receiver ! FileWriteException(cmd.tableName)}
  }
}

object TableProcessor {
  def props = Props[TableProcessor]
}

case class FileWriteException(name:String) extends Exception(s"exception during writing to $name")

