package org.comsoft

import akka.testkit.TestKit
import org.comsoft.config.{ConfiguredDBs, ConfigLoader}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import scalikejdbc.config.DBs

/**
 * Created by alexgri on 25.12.14.
 */
trait TestBase extends FlatSpecLike with BeforeAndAfterAll {
  self: TestKit with AdditionalConfigFile =>

  lazy val customConfig = ConfigLoader.configFromFile(userConfigFile)
  lazy val configuredDBs = new ConfiguredDBs(customConfig)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    configuredDBs.setupAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    configuredDBs.closeAll()
    TestKit.shutdownActorSystem(system)
  }
}
