package org.comsoft.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

/**
 * Created by alexgri on 04.03.15.
 */
object ConfigLoader {
  def passedConfig(arguments: Array[String]):Config = {
    Try {
      configFromFile(arguments(0))
    }.getOrElse(throw new IllegalArgumentException(" the path to configuration file should be provided! "))
  }

  def configFromFile(file : String) = ConfigFactory.parseFile(new File(file))

  def customizedConfig(arguments: Array[String]) = passedConfig(arguments).withFallback(ConfigFactory.load()).resolve()
}
