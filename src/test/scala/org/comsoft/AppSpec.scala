package org.comsoft

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.comsoft.Protocol._
import org.scalatest.Matchers
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

  val toPG = system.actorOf(CopyToPostgres.props, "ctp")

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

  it should " copy from csv " in {
    val doc = getClass.getResource("/ERM_DOCUMENT-0.csv")
    toPG ! CopyTo(doc.getFile, "ERM_DOCUMENT" , Seq("ID", "FOLDER_ID", "BRANCH", "STATUS", "DOCTYPE", "DOCKIND", "REGDATE", "AGENT", "AGENTREQUEST", "INTCORRESPONDENT", "BOARD", "AUTHOR", "DELIVERYKIND", "NOMENCLATURE", "REGISTRATOR", "STARTDATE", "ENDDATE", "SIGNER", "OUTDATE",
      "STORAGE", "TRANSFERDATE", "CREATEDATE", "SUPERVISOR", "INTCONTRAGENT", "NEEDAGREEMENT", "ARCHIVE", "ORIGINAL", "CLERKNOTE", "SUBJECT",
      "NUMBER", "OUTNUMBER", "ASUD", "INAUTHOR"))
    expectMsg(Copied(doc.getFile))
  }

}
