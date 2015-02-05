package org.comsoft

import java.nio.file.Path

/**
 * Created by alexgri on 02.02.15.
 */
object Protocol {
  case class DoExport(tables:List[String])
  case class TableInfo(name:String, blobFields:Seq[String], regularFields:Seq[String], batches:Seq[BatchInfo])
  case class BatchInfo(selectPart:String, query:String, csvPath:Path)
  case class BatchPart(table:String, info:BatchInfo)
  //case class Batches(table:String, batches: Seq[BatchPart])
  case class Process(table:String)
  case class WorkDone(tableName:String)
  case class WorkComplete(infos:Seq[TableInfo])
  case object Collect
  case class CopyTo(filename:String, table:String, columns:Seq[String])
  case class Copied(filename:String)
  case object CopyBlobs
  case class CopyBlob(table:String, columns:Seq[String], selectPart: String)
  case class BlobsCopied(name:String)

}
