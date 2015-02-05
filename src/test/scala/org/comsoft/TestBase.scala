package org.comsoft

import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import scalikejdbc.config.DBs

/**
 * Created by alexgri on 25.12.14.
 */
trait TestBase extends FlatSpecLike with BeforeAndAfterAll {
  self: TestKit =>

  def cfg = system.settings.config


  override protected def beforeAll(): Unit = {
    super.beforeAll()
    DBs.setupAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    DBs.closeAll()
    TestKit.shutdownActorSystem(system)
  }
}
