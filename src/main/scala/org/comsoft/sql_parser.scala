package org.comsoft

import akka.actor.ActorContext

import scala.annotation.tailrec

/**
 * Created by yakupov on 04.03.2015.
 */
class SqlParser(sqlMetadata: => String) extends App {

  lazy val blocks = sqlMetadata.split(";")
  lazy val (used, _) = blocks.span(p => !p.contains("COMMIT WORK"))

  @tailrec
  private def traverse(list: List[String], l1: List[String], l2: List[String], l3: List[String], l4: List[String]): ParseResult = {
    list match {
      case Nil => 
        val r1 = l1.map(x => processGenerator(commonProcessing(x+";")))
        val r2 = l2.map(x => processTable(commonProcessing(x+";")))
        val r3 = l3.map(x => processIndex(commonProcessing(x+";")))       
        val r4 = l4.map(x => ConstraintsDefinition(commonProcessing(x+";")))
        ParseResult(r1,r2,r3,r4)
      case y :: ys =>
        y match {
          case x if x.matches("(?i)(?s).*CREATE *GENERATOR.*") => traverse(ys, x :: l1, l2, l3, l4)
          case x if x.matches("(?i)(?s).*CREATE *TABLE.*") => traverse(ys, l1, x :: l2, l3, l4)
          case x if x.matches("(?i)(?s).*CREATE *(DESCENDING)* *INDEX.*") => traverse(ys, l1, l2, x :: l3, l4)
          case x if x.matches("(?i)(?s).*ALTER *TABLE.*") => traverse(ys, l1, l2, l3, x :: l4)
          case _ =>  traverse(ys, l1, l2, l3, l4)
        }

    }
  }

  /**
   * GENERATOR -> SEQUENCE
   */
  //CREATE GENERATOR ATTACHMENT_ID_SEQ dasd;
  def processGenerator(sql:String):SequenceDefinition = {
    val pattern = "(?i)(?s).*CREATE *GENERATOR *(\\w*)(.*)".r  .findFirstMatchIn(sql)
    val name = pattern.map(_ group 1).get
    val data = pattern.map(_ group 2).get
    val post_sql = "CREATE SEQUENCE " + name + data
    SequenceDefinition(name, post_sql )
  }

  /**
   * BLOB SUB_TYPE TEXT SEGMENT SIZE \d* -> text
  BLOB SUB_TYPE 0 SEGMENT SIZE \d* -> OID
    NUMERIC(18, 0) -> BIGINT
   * */
  def processTable(sql:String):TableDefinition = {
    val pattern = "(?i)(?s).*CREATE *TABLE *(\\w*)(.*)".r  .findFirstMatchIn(sql)
    val name = pattern.map(_ group 1).get
    val p1 = "(?i)(?s)BLOB *SUB_TYPE *TEXT *SEGMENT *SIZE *\\d*"
    val p2 = "(?i)(?s)BLOB *SUB_TYPE *0 *SEGMENT *SIZE *\\d*"
    val p3 = "(?i)(?s)NUMERIC *\\( *18 *, *0 *\\)"
    val data = pattern.map(_ group 2).get.replaceAll(p1, "TEXT").replaceAll(p2, "OID").replaceAll(p3, "BIGINT")
    val post_sql = "CREATE TABLE " + name + data
    TableDefinition(name, post_sql)
  }

  def processIndex(sql: String): IndexDefinition = {
    val pattern = "(?i)(?s).*CREATE *(DESCENDING)* *INDEX *(.*)\\((.*)\\)(.*)".r  .findFirstMatchIn(sql)
    val desc = pattern.map(_ group 1) match {
      case Some("DESCENDING") => " DESC"
      case _ => ""
    }
    val g1 = pattern.map(_ group 2).get.replaceAll("(?i)(?s) *COMPUTED *BY", "")
    val g2 = pattern.map(_ group 3).get
    val g3 = pattern.map(_ group 4).get
    IndexDefinition(s"CREATE INDEX $g1($g2$desc)$g3")
  }

  //  удалить кавычки "
  def commonProcessing(sql:String):String = sql.replaceAll("\"","")


  def traverse:ParseResult = traverse(used.toList, Nil, Nil, Nil, Nil)
}

object SqlParser {
  def apply(implicit context:ActorContext) = new SqlParser(TableMetadataExtractor(context).metadata)
}

//case class ParseResult(sequences:List[String], tables:List[String], indexesAndConstraints:List[String])
case class ParseResult(sequences:List[SequenceDefinition], tables:List[TableDefinition], indexes:List[IndexDefinition], constraints: List[ConstraintsDefinition])

case class SequenceDefinition(name:String, sql:String)
case class TableDefinition(name:String, sql:String)
case class IndexDefinition(sql:String)
case class ConstraintsDefinition(sql:String)

