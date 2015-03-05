package org.comsoft.config

import java.io.File

import com.typesafe.config.{ConfigResolveOptions, ConfigParseOptions, Config, ConfigFactory}

import scala.util.Try

/**
 * Created by alexgri on 04.03.15.
 */
object ConfigLoader {
  private val defaultException = new IllegalArgumentException(" the path to configuration file should be provided! ")

  def passedConfig(file : String):Try[Config] = Try(configFromFile(file))
  def configFromFile(file : String) = ConfigFactory.parseFile(new File(file))
  def combinedConfig(file : String) = passedConfig(file).map(_.withFallback(ConfigFactory.load("application.conf", ConfigParseOptions.defaults(), ConfigResolveOptions.defaults().setAllowUnresolved(true))).resolve())
  def customizedConfig(file : String) = combinedConfig(file).getOrElse(throw defaultException)


  def customizedConfig(arguments: Array[String]):Config = {
    val cfg = Try(arguments(0)).flatMap(combinedConfig)
    cfg.getOrElse(throw defaultException)
  }
}
