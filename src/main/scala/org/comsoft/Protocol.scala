package org.comsoft

/**
 * Created by alexgri on 02.02.15.
 */
object Protocol {
  case class DoExport(tables:List[String])
  case class TableInfo(name:String, batches:Seq[BatchInfo])
  case class BatchInfo(selectQuery:String, insertQuery:String)
  case class BatchPart(table:String, info:BatchInfo)
  case class Process(table:String)
  case class WorkDone(tableName:String)
  case object WorkComplete
  case object Collect
}
