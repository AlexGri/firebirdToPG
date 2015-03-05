package org.comsoft

import akka.actor.{Props, Actor, ActorLogging}
import org.comsoft.Protocol.ExtractMetadata

/**
 * Created by alexgri on 05.03.15.
 */
class MetadataActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case ExtractMetadata =>
      val parser = SqlParser.apply
      parser.traverse
  }
}

object MetadataActor {
  def props = Props[MetadataActor]
}
