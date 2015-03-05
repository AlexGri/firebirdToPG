package org.comsoft

import org.comsoft.config.ConfigLoader
import org.scalatest.{Matchers, FreeSpec}

/**
  * Created by alexgri on 04.03.15.
  */
class ConfigLoadingSpec extends FreeSpec with Matchers {

   "Config loader should receive path to userconfig and make combined one" in {
     val userConfigFile = this.getClass.getResource("/userConfig.conf").getFile
     val config = ConfigLoader.customizedConfig(Array(userConfigFile))
     config.getString("database.name") shouldBe "aisbd-initial"
     config.getString("db.default.url") shouldBe "jdbc:firebirdsql:localhost/3050:aisbd-initial?lc_ctype=WIN1251"
     config.getInt("scalikejdbc.global.loggingSQLAndTime.stackTraceDepth") shouldBe 15
   }
 }
