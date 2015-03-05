package org.comsoft

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import org.comsoft.config.{ConfigLoader, ConfiguredDBs}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

/**
 * Created by alexgri on 25.12.14.
 */
trait TestBase extends FlatSpecLike with BeforeAndAfterAll with TestKitBase {
  self: AdditionalConfigFile =>

  lazy val customConfig = ConfigLoader.customizedConfig(userConfigFile)
  lazy val configuredDBs = new ConfiguredDBs(customConfig)

  implicit lazy val system:ActorSystem = ActorSystem("TestSystem", customConfig)

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
