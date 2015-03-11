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
    val ParseResult(sequences, tables, indexes, constraints) = parser.traverse

    //println(sequences.zipWithIndex.mkString("\n"))
    //println(tables.zipWithIndex.mkString("\n"))
    //println(indexesAndConstraints.zipWithIndex.mkString("\n"))

    sequences.size shouldBe 14
    tables.size shouldBe 119
    indexes.size shouldBe 44
    constraints.size shouldBe 148


  }

  "convert generator to sequence" in {
    val inSql = "CREATE GENERATOR ATTACHMENT_ID_SEQ;"
    val outSql = "CREATE SEQUENCE ATTACHMENT_ID_SEQ;"

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse
        //compare with outSql
    s.head.sql shouldBe outSql
  }

  "replace numeric(18, 0) with bigint" in {
    val inSql = """CREATE TABLE CFG_RESOURCE (ID NUMERIC(18, 0) NOT NULL,
                  |        PATH VARCHAR(255) NOT NULL,
                  |        DESCRIPTION VARCHAR(255) NOT NULL,
                  |        INSTALL_ORDER NUMERIC(18, 0) NOT NULL,
                  |        RESOURCE_TYPE NUMERIC(18, 0) NOT NULL,
                  |        ARTIFACT NUMERIC(18, 0) NOT NULL,
                  |PRIMARY KEY (ID));""".stripMargin

    val outSql = """CREATE TABLE CFG_RESOURCE (ID BIGINT NOT NULL,
                   |        PATH VARCHAR(255) NOT NULL,
                   |        DESCRIPTION VARCHAR(255) NOT NULL,
                   |        INSTALL_ORDER BIGINT NOT NULL,
                   |        RESOURCE_TYPE BIGINT NOT NULL,
                   |        ARTIFACT BIGINT NOT NULL,
                   |PRIMARY KEY (ID));""".stripMargin

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse

    //compare with outSql
    t.head.sql shouldBe outSql
  }

  "numeric(18, 2) should not be replaced" in {
    val inSql = """CREATE TABLE CFG_RESOURCE (ID NUMERIC(18, 2) NOT NULL,
                  |        PATH VARCHAR(255) NOT NULL,
                  |        DESCRIPTION VARCHAR(255) NOT NULL,
                  |        INSTALL_ORDER NUMERIC(18, 0) NOT NULL,
                  |        RESOURCE_TYPE NUMERIC(18, 0) NOT NULL,
                  |        ARTIFACT NUMERIC(18, 0) NOT NULL,
                  |PRIMARY KEY (ID));""".stripMargin

    val outSql = """CREATE TABLE CFG_RESOURCE (ID NUMERIC(18, 2) NOT NULL,
                   |        PATH VARCHAR(255) NOT NULL,
                   |        DESCRIPTION VARCHAR(255) NOT NULL,
                   |        INSTALL_ORDER BIGINT NOT NULL,
                   |        RESOURCE_TYPE BIGINT NOT NULL,
                   |        ARTIFACT BIGINT NOT NULL,
                   |PRIMARY KEY (ID));""".stripMargin

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse

    //compare with outSql
    t.head.sql shouldBe outSql
  }

  "remove computed by from index" in {
    val inSql = "CREATE INDEX IX_DOCKIND_ANAME_UP ON ERM_DOCKIND COMPUTED BY (upper(ANAME));"
    val outSql = "CREATE INDEX IX_DOCKIND_ANAME_UP ON ERM_DOCKIND (upper(ANAME));"

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse

    //compare with outSql
    i.head.sql shouldBe outSql
  }

  "replace desc keyword" in {
    val inSql = "CREATE DESCENDING INDEX ERM_FOLDER_IDX2 ON ERM_FOLDER (RBORDER);"
    val outSql = "CREATE INDEX ERM_FOLDER_IDX2 ON ERM_FOLDER (RBORDER DESC);"

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse

    //compare with outSql
    i.head.sql shouldBe outSql
  }

  "convert text type" in {
    val inSql = """CREATE TABLE CFG_ARTIFACT (ID NUMERIC(18, 0) NOT NULL,
                  |        NAME VARCHAR(255) NOT NULL,
                  |        CODE VARCHAR(30) NOT NULL,
                  |        INSTALLED SMALLINT,
                  |        INSTALL_LOG BLOB SUB_TYPE TEXT SEGMENT SIZE 80,
                  |PRIMARY KEY (ID));""".stripMargin
    val outSql = """CREATE TABLE CFG_ARTIFACT (ID BIGINT NOT NULL,
                   |        NAME VARCHAR(255) NOT NULL,
                   |        CODE VARCHAR(30) NOT NULL,
                   |        INSTALLED SMALLINT,
                   |        INSTALL_LOG TEXT,
                   |PRIMARY KEY (ID));""".stripMargin

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse

    //compare with outSql
    t.head.sql shouldBe outSql
  }

  "convert blob type" in {
    val inSql = """CREATE TABLE ERMS_SAVED_FILTER (
                  |        NAME VARCHAR(250) NOT NULL,
                  |        XMLDATA BLOB SUB_TYPE 0 SEGMENT SIZE 80 NOT NULL,
                  |CONSTRAINT PK_ERMS_SAVED_FILTER PRIMARY KEY (ID));""".stripMargin
    val outSql = """CREATE TABLE ERMS_SAVED_FILTER (
                   |        NAME VARCHAR(250) NOT NULL,
                   |        XMLDATA BYTEA NOT NULL,
                   |CONSTRAINT PK_ERMS_SAVED_FILTER PRIMARY KEY (ID));""".stripMargin

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse

    //compare with outSql
    t.head.sql shouldBe outSql
  }

  "replace quotes in names" in {
    val inSql = """CREATE TABLE ERM_AGENTREQUEST (
                  |        SURNAME VARCHAR(64),
                  |        "FIRSTNAME" VARCHAR(64),
                  |        PATRONYMIC VARCHAR(64),
                  |        ANAME VARCHAR(255),
                  |        OPF VARCHAR(64),
                  |        TITLE VARCHAR(255)
                  |CONSTRAINT PK_ERM_AGENTREQUEST PRIMARY KEY (ID));""".stripMargin
    val outSql = """CREATE TABLE ERM_AGENTREQUEST (
                   |        SURNAME VARCHAR(64),
                   |        FIRSTNAME VARCHAR(64),
                   |        PATRONYMIC VARCHAR(64),
                   |        ANAME VARCHAR(255),
                   |        OPF VARCHAR(64),
                   |        TITLE VARCHAR(255)
                   |CONSTRAINT PK_ERM_AGENTREQUEST PRIMARY KEY (ID));""".stripMargin

    val parser = new SqlParser(inSql)
    val ParseResult(s, t, i, c) = parser.traverse

    //compare with outSql
    t.head.sql shouldBe outSql
  }
}
