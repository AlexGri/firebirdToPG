package org.comsoft

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.routing.FromConfig
import org.comsoft.Protocol._
import scalikejdbc.config.DBs

class MainActor extends Actor {

  val manager = context.actorOf(WorkManager.props, "operator")
  val allTables = context.actorOf(TableRetriever.props, "tr")
  val postgres = context.actorOf(FromConfig.props(CopyToPostgres.props), "pg")

  var infos:Seq[TableInfo] = Seq.empty
  var filesToCopy:Set[String] = Set.empty
  var blobsToCopy:Map[String, Int] = Map.empty

  context.watch(manager)
  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(){
      case _ => Stop
    }
  }

  override def receive: Receive = {
    case Collect => allTables ! Collect
    case WorkComplete(tableInfos) =>
      infos = tableInfos
      val copyCommands = tableInfos.flatMap{case TableInfo(name, _, fields, batches) =>
        batches.map(b => CopyTo(b.csvPath.toString, name, fields))
      }
      filesToCopy = copyCommands.map(_.filename).toSet
      copyCommands.foreach(postgres ! _)
    case Copied(filename) =>
      filesToCopy = filesToCopy - filename
      if (filesToCopy.isEmpty) self ! CopyBlobs
    case msg: DoExport => manager ! msg
    case CopyBlobs =>
      val tablesWithBlobs = infos.filter(ti => ti.blobFields.nonEmpty)
      blobsToCopy = tablesWithBlobs.map(ti => ti.name -> ti.batches.size).toMap
      val cmds = tablesWithBlobs.flatMap(ti => ti.batches.map(bi => CopyBlob(ti.name, ti.blobFields, bi.selectPart)) )
      cmds.foreach(postgres ! _)
    case BlobsCopied(name) =>
      val num =  blobsToCopy.getOrElse(name, 0) - 1
      blobsToCopy = if (num <= 0) blobsToCopy - name
      else blobsToCopy.updated(name, num)
      if (blobsToCopy.isEmpty) context.system.shutdown()
    case Terminated(manager) => context.system.shutdown()
  }
}

object Main extends App {
  DBs.setupAll()
  val system = ActorSystem("example")

  sys.addShutdownHook {
    DBs.closeAll()
    system.shutdown()
  }

  val main = system.actorOf(Props[MainActor], "main")

  main ! Collect
}