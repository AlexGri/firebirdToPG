package org.comsoft

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.Matchers

/**
 * Created by alexgri on 04.03.15.
 */
class MetadataSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with TestBase with Matchers with TestConfigFile {

  behavior of "Metadata"

  def this() = this(ActorSystem("ExportSystem", customConfig))

  it should "be retrieved from db" in {
    val extractor = TableMetadataExtractor(_system.settings.config)
    println(extractor.cmdString)
    val meta = extractor.metadata
    println(meta)
    meta.isEmpty shouldBe false
  }
}
