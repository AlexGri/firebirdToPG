package org.comsoft

import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.comsoft.Protocol._
import org.scalatest.Matchers
import scalikejdbc._
import scala.concurrent.duration._

/**
 * Created by alexgri on 02.02.15.
 */
class AppSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with TestBase with Matchers  {
  behavior of "App"

  def this() = this(ActorSystem("ExportSystem"))

  def createManager = system.actorOf(WorkManager.props, "operator")

  val manager = createManager
  val allTables = system.actorOf(TableRetriever.props, "tr")

  it should "" ignore {
    allTables ! Collect
    val msg = expectMsgType[DoExport]
    msg.tables.size shouldBe 190
    val msg2 = msg.copy(msg.tables.take(10))
    msg2.tables.size shouldBe 10
    manager ! msg
    //manager ! msg2
    expectMsg(5 minutes,  WorkComplete)
  }

 /* it should " copy from csv " in {
    val doc = getClass.getResource("/ERM_DOCUMENT-0.csv")
    toPG ! CopyTo(doc.getFile, "ERM_DOCUMENT" , Seq("ID", "FOLDER_ID", "BRANCH", "STATUS", "DOCTYPE", "DOCKIND", "REGDATE", "AGENT", "AGENTREQUEST", "INTCORRESPONDENT", "BOARD", "AUTHOR", "DELIVERYKIND", "NOMENCLATURE", "REGISTRATOR", "STARTDATE", "ENDDATE", "SIGNER", "OUTDATE",
      "STORAGE", "TRANSFERDATE", "CREATEDATE", "SUPERVISOR", "INTCONTRAGENT", "NEEDAGREEMENT", "ARCHIVE", "ORIGINAL", "CLERKNOTE", "SUBJECT",
      "NUMBER", "OUTNUMBER", "ASUD", "INAUTHOR"))
    expectMsg(Copied(doc.getFile))
  }*/

  val charsetsNames = Seq("UTF-8", "windows-1251", "KOI8-R", "CP866", "ISO-8859-5")
  val charsets = charsetsNames.map(Charset.forName)
  val r:Seq[(Charset, Charset)] = charsets.combinations(2)
    .flatMap(_.permutations)
    .map{case c1::c2::_ => (c1, c2)}.toSeq
  val identity = charsets.map(c => (c,c))




  it should  "correctly convert string with null symbol " in {

    val id = 972956
    val query = sql"select subject_id FROM oc_jbpm5ext_attribute WHERE id = $id"
    val str = DB readOnly { implicit session =>
      query.map(rs => rs.string(1)).single().apply().getOrElse("")
    }
    val converted = (identity ++ r).map{ case (c1, c2) => s"$c1 -> $c2 = " + new String(str.getBytes(c1), c2)}
    println(converted.mkString("\n"))
  }

}
