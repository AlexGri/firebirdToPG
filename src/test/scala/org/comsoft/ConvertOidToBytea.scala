package org.comsoft

import org.scalatest.{Matchers, FreeSpec}

/**
 * Created by alexgri on 25.03.15.
 */
class ConvertOidToBytea extends FreeSpec with Matchers {
  def loadMetadata: String = {
    val source = scala.io.Source.fromURL(this.getClass.getResource("/aisbdnew.sql"))
    val sql = source.mkString
    source.close()
    sql
  }

  def script(tName:String, fieldName:String, nullability:Option[String] ) =
    s"""
      |ALTER table $tName ADD COLUMN TMP bytea${nullability.getOrElse("")};
      |update $tName set tmp = merge_oid($fieldName);
      |ALTER table $tName DROP COLUMN $fieldName;
      |ALTER table $tName RENAME COLUMN TMP to $fieldName;
    """.stripMargin

  "parse sql metadata" ignore {
    val sql = loadMetadata
    val parser = new SqlParser(sql)
    val ParseResult(_, tables, _, _) = parser.traverse

    tables.size shouldBe 219

    val pattern = """(?:\(|\s)(\w+)\sOID( NOT NULL)*""".r
    val sc = tables.map{
      case TableDefinition(name, sql) => pattern.findFirstMatchIn(sql).map(fld => (name, fld.group(1), Option(fld.group(2))))
    }.collect {
      case Some((name, fld, option)) => script(name, fld, option)
    }

    println(sc)
  }


  "convert smallint to boolean" in {
    val sql = loadMetadata
    val parser = new SqlParser(sql)
    val ParseResult(_, tables, _, _) = parser.traverse

    tables.size shouldBe 219
    val pattern = """(\w+)\sSMALLINT""".r
    val sc = tables.flatMap{
      case TableDefinition(name, sql) => pattern.findAllMatchIn(sql).map(fld => alterToBoolean(name, fld.group(1)))
    }

    println(sc.mkString("\n"))
  }

  def alterToBoolean(tableName:String, fieldName:String) = s"alter table $tableName ALTER COLUMN $fieldName TYPE boolean using $fieldName = 1;"
}
