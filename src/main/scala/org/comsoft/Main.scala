package org.comsoft

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import org.comsoft.Protocol._
import org.comsoft.config.{ConfiguredDBs, ConfigLoader}
import scalikejdbc.config.DBs

class MainActor extends Actor with ActorLogging {

  val metaActor = context.actorOf(MetadataActor.props, "meta")
  val manager = context.actorOf(WorkManager.props, "operator")
  val allTables = context.actorOf(TableRetriever.props, "tr")
  var pgtime = 0l
  var fbtime = 0l
  var ddl: PostMigrateDDL = _
  context.watch(manager)
  override def supervisorStrategy: SupervisorStrategy = {
    AllForOneStrategy(){
      case msg => log.info(s"!!!!!! $msg"); Stop
    }
  }

  override def receive: Receive = {
    case ExtractMetadata => metaActor ! ExtractMetadata
    case p : PostMigrateDDL =>
      ddl = p
      self ! Collect
    case Collect => allTables ! Collect
    case WorkComplete =>
      log.info("data migration completed")
      log.info(s"pg time $pgtime")
      log.info(s"fb time $fbtime")
      pgtime = 0
      metaActor ! ddl
    case IndexesCreated =>
      log.info(s"post migration ddl completed in $pgtime")
      shutdown
    case msg: DoExport => manager ! msg
    case Terminated(manager) => shutdown
    case PGTime(time) => pgtime += time
    case FBTime(time) => fbtime += time
  }

  def shutdown = {
    log.info("shutting down")
    Main.closeAll()
    context.system.shutdown()
  }
}

object Main extends App {  
  val config = ConfigLoader.customizedConfig(args)
  val DBS = new ConfiguredDBs(config)
  DBS.setupAll()
  val system = ActorSystem("example", config)
  sys.addShutdownHook {
    DBS.closeAll()
    system.shutdown()
  }
  val main = system.actorOf(Props[MainActor], "main")
  main ! ExtractMetadata

  def closeAll() = DBS.closeAll()
}