package org.comsoft

import akka.actor.ActorSystem
import com.typesafe.config.Config

import sys.process._

/**
 * Created by alexgri on 04.03.15.
 */
class TableMetadataExtractor(pathToIsql:String, fbUser:String, fbPassword:String, dbName:String) {

  def cmdString = s"$pathToIsql -x -u $fbUser -p $fbPassword -ch WIN1251 $dbName"
  def metadata:String = cmdString !!

}

object TableMetadataExtractor {
  def apply(config:Config) = {
    new TableMetadataExtractor(config.getString("db.default.isql"),
      config.getString("db.default.user"),
      config.getString("db.default.password"),
      config.getString("database.name"))
  }

  def apply(implicit actorSystem: ActorSystem): TableMetadataExtractor = this.apply(actorSystem.settings.config)
}
