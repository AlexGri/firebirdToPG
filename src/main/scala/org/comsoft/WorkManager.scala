package org.comsoft

import akka.actor.SupervisorStrategy.{Decider, Escalate, Stop}
import akka.actor._
import akka.routing.FromConfig
import org.comsoft.Protocol._

/**
 * Created by alexgri on 02.02.15.
 */
class WorkManager extends Actor {
  var todo:Map[String, Int] = _
  var requestor:ActorRef = _
  var tableInfos:Seq[TableInfo] = Seq.empty
  val processor = createProcessor
  val infoAggregator = createInfoAggregator

  val decider: Decider = {
    case _:FileWriteException => Escalate
    case _: ActorInitializationException ⇒ Stop
    case _: ActorKilledException         ⇒ Stop
    case _: DeathPactException           ⇒ Stop
    case _: Exception                    ⇒ Stop
  }

  override def supervisorStrategy: SupervisorStrategy = {
    AllForOneStrategy()(decider)
  }

  def createProcessor = context.actorOf(FromConfig.props(TableProcessor.props), "processor")
  def createInfoAggregator = context.actorOf(FromConfig.props(InfoAggregator.props), "info")

  def receive: Receive = {
    case DoExport(tables) =>
      requestor = sender()
      todo = tables.map(t => t -> 1).toMap
      tables.foreach{case table => infoAggregator ! Process(table)}
    case WorkDone(tableName) =>
      val cnt = todo.getOrElse(tableName, 0) - 1
      if (cnt <= 0)
        todo = todo - tableName
      else
        todo = todo.updated(tableName, cnt)
      if (todo.isEmpty) requestor ! WorkComplete(tableInfos)
    case ti@TableInfo(table, blobs, regular, batchInfos) =>
      todo = todo.updated(table, batchInfos.size)
      tableInfos = tableInfos :+ ti
      batchInfos.foreach(processor ! _)
    case e:FileWriteException => throw e
  }
}

object WorkManager {
  def props = Props[WorkManager]
}
