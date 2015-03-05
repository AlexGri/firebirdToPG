package org.comsoft

import scala.annotation.tailrec

/**
 * Created by yakupov on 04.03.2015.
 */
class SqlParser(sqlMetadata: => String) extends App {

  lazy val blocks = sqlMetadata.split(";")
  lazy val (used, _) = blocks.span(p => !p.contains("COMMIT WORK"))

  @tailrec
  private def traverse(list: List[String], l1: List[String], l2: List[String], l3: List[String]): ParseResult = {
    list match {
      case Nil => ParseResult(l1.map(_ + ";"), l2.map(_ + ";"), l3.map(_ + ";"))
      case y :: ys =>
        y match {
          case x if x.matches("(?i)(?s).*CREATE *GENERATOR.*") => traverse(ys, x :: l1, l2, l3)
          case x if x.matches("(?i)(?s).*CREATE *TABLE.*") => traverse(ys, l1, x :: l2, l3)
          case x if x.matches("(?i)(?s).*CREATE *INDEX.*") => traverse(ys, l1, l2, x :: l3)
          case x if x.matches("(?i)(?s).*ALTER *TABLE.*") => traverse(ys, l1, l2, x :: l3)
          case _ =>  traverse(ys, l1, l2, l3)
        }

    }
  }

  /**
   * GENERATOR -> SEQUENCE
   */
  def processGenerator(sql:String):SequenceDefinition = ???

  /**
   * BLOB SUB_TYPE TEXT SEGMENT SIZE \d* -> text
  BLOB SUB_TYPE 0 SEGMENT SIZE \d* -> OID
    NUMERIC(18, 0) -> BIGINT
   * */
  def processTable(sql:String):TableDefinition = ???

  //  удалить кавычки "
  def commonProcessing(sql:String):String = ???


  def traverse:ParseResult = traverse(used.toList, Nil, Nil, Nil)
}

case class ParseResult(sequences:List[String], tables:List[String], indexesAndConstraints:List[String])
//case class ParseResult(sequences:List[SequenceDefinition], tables:List[TableDefinition], indexesAndConstraints:List[String])

case class SequenceDefinition(name:String, sql:String)
case class TableDefinition(name:String, sql:String)
