package org.comsoft

import akka.testkit.ImplicitSender
import org.scalatest.Matchers

/**
 * Created by alexgri on 04.03.15.
 */
class MetadataSpec extends TestConfigFile with TestBase with Matchers with ImplicitSender {

  behavior of "Metadata"

  it should "be retrieved from db" in {
    val extractor = TableMetadataExtractor(system.settings.config)
    println(extractor.cmdString)
    val meta = extractor.metadata
    println(meta)
    meta.isEmpty shouldBe false
  }
}
