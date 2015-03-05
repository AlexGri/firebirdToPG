package org.comsoft

import org.scalatest.{Matchers, FreeSpec}

/**
 * Created by alexgri on 05.03.15.
 */
class SqlParserSpec extends FreeSpec with Matchers {
  def loadMetadata: String = {
    val source = scala.io.Source.fromURL(this.getClass.getResource("/aisbd-initial.sql"))
    val sql = source.mkString
    source.close()
    sql
  }

  "parse sql metadata" in {
    val sql = loadMetadata
    val parser = new SqlParser(sql)
    val ParseResult(sequences, tables, indexesAndConstraints) = parser.traverse

    //println(sequences.zipWithIndex.mkString("\n"))
    //println(tables.zipWithIndex.mkString("\n"))
    //println(indexesAndConstraints.zipWithIndex.mkString("\n"))

    sequences.size shouldBe 14
    tables.size shouldBe 119
    indexesAndConstraints.size shouldBe 44 + 148

  }
}
