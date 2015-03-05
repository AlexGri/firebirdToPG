package org.comsoft

/**
 * Created by alexgri on 05.03.15.
 */
trait AdditionalConfigFile {
  def userConfigFile:String
}

trait TestConfigFile extends AdditionalConfigFile {
  val userConfigFile = this.getClass.getResource("/userConfig.conf").getFile
}
