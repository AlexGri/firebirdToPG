package org.comsoft

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.routing.FromConfig
import org.comsoft.Protocol._
import scalikejdbc.config.DBs

class MainActor extends Actor with ActorLogging {

  val manager = context.actorOf(WorkManager.props, "operator")
  val allTables = context.actorOf(TableRetriever.props, "tr")
  var pgtime = 0l
  var fbtime = 0l
  context.watch(manager)
  override def supervisorStrategy: SupervisorStrategy = {
    AllForOneStrategy(){
      case msg => log.info(s"!!!!!! $msg"); Stop
    }
  }

  override def receive: Receive = {
    case Collect => allTables ! Collect
    case WorkComplete =>
      log.info("all work completed");
      log.info(s"pg time $pgtime");
      log.info(s"fb time $fbtime");
      context.system.shutdown()
    case msg: DoExport => manager ! msg
    case Terminated(manager) => log.info("shutting down"); context.system.shutdown()
    case PGTime(time) => pgtime = time
    case FBTime(time) => fbtime = time
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