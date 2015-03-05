package org.comsoft

import java.sql.Blob

/**
 * Created by alexgri on 02.02.15.
 */
object Protocol {
  case class DoExport(val tables:List[String]) extends AnyVal
  sealed trait TI {
    def name:String
    def batches:Seq[Batch]
  }

  case class TableInfo(name:String, batches:Seq[BatchInfo]) extends TI
  case class BlobTableInfo(name:String, batches:Seq[BlobBatchInfo]) extends TI

  sealed trait Batch {
    def selectQuery:String
    def insertQuery:String
  }
  case class BatchInfo(selectQuery:String, insertQuery:String) extends Batch
  case class BlobBatchInfo(selectQuery:String, insertQuery:String) extends Batch
  case class BatchPart(table:String, info:Batch)
  case class Process(val table:String) extends AnyVal
  case class WorkDone(val tableName:String) extends AnyVal
  case object WorkComplete
  case object Collect
  case class BlobBytes(val bytes:Array[Byte]) extends AnyVal
  object BlobBytes {
    def apply(blob:Blob) = new BlobBytes(blob.getBytes(1, blob.length().toInt))
  }
  case object BlobProcessed

  case object ExtractMetadata
}
