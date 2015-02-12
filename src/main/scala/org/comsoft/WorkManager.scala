package org.comsoft

import akka.actor.SupervisorStrategy.{Escalate, Decider, Stop}
import akka.actor._
import akka.routing.{BalancingPool, FromConfig}
import org.comsoft.Protocol._

/**
 * Created by alexgri on 02.02.15.
 */
class WorkManager extends Actor with ActorLogging {
  var todo:Map[String, Int] = _
  var requestor:ActorRef = _
  var tableInfos:Seq[TI] = Seq.empty
  val processor = createProcessor
  val infoAggregator = createInfoAggregator


  val decider: Decider = {
    case _ ⇒ Escalate
  }

  override def supervisorStrategy: SupervisorStrategy = {
    AllForOneStrategy()(decider)
  }

  def createProcessor = context.actorOf(FromConfig.props(TableProcessor.props), "processor")
  def createInfoAggregator = context.actorOf(FromConfig.props(InfoAggregator.props), "info")
  //val blobsaver = context.actorOf(BalancingPool(5).props(BlobSaver.props), "blobsaver")

  def receive: Receive = {
    case DoExport(tables) =>
      requestor = sender()
      todo = tables.map(t => t -> 1).toMap
      tables.foreach{case table => infoAggregator ! Process(table)}
    case WorkDone(tableName) =>
      val cnt = todo.getOrElse(tableName, 0) - 1
      if (cnt <= 0) {
        log.info(s"processing of $tableName is completed")
        todo = todo - tableName
      } else {
        log.info(s"$tableName: $cnt batches left")
        todo = todo.updated(tableName, cnt)
      }
      sendIfComplete
    case ti@TableInfo(table, batchInfos) =>
      todo = todo.updated(table, batchInfos.size)
      tableInfos = tableInfos :+ ti
      batchInfos.foreach(bi => processor ! BatchPart(table, bi))
      //self ! WorkDone(table)
    case ti@BlobTableInfo(table, batchInfos) =>
      log.info(s"$table contains blob fields")
      todo = todo.updated(table, batchInfos.size)
      tableInfos = tableInfos :+ ti
      batchInfos.foreach(bi => processor ! BatchPart(table, bi))
    case t:TimingMsg => context.parent ! t
   /* case b:BlobBytes =>
      numOfBlobsToProcess = numOfBlobsToProcess + 1
      blobsaver ! b
    case BlobProcessed =>
      numOfBlobsToProcess = numOfBlobsToProcess - 1
      sendIfComplete*/
  }

  def sendIfComplete = if (todo.isEmpty) requestor ! WorkComplete
}

object WorkManager {
  def props = Props[WorkManager]
}
