package org.comsoft

import java.sql.Blob

/**
 * Created by alexgri on 02.02.15.
 */
object Protocol {
  case class DoExport(val tables:List[String]) extends AnyVal
  case class TableInfo(name:String, batches:Seq[BatchInfo])
  case class BatchInfo(selectQuery:String, insertQuery:String)
  case class BatchPart(table:String, info:BatchInfo)
  case class Process(val table:String) extends AnyVal
  case class WorkDone(val tableName:String) extends AnyVal
  case object WorkComplete
  case object Collect
  case class BlobBytes(val bytes:Array[Byte]) extends AnyVal
  object BlobBytes {
    def apply(blob:Blob) = new BlobBytes(blob.getBytes(1, blob.length().toInt))
  }
  case object BlobProcessed
}
