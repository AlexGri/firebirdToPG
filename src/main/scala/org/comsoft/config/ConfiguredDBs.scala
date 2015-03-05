package org.comsoft.config

import com.typesafe.config.Config
import scalikejdbc.config.{NoEnvPrefix, TypesafeConfigReader, TypesafeConfig, DBs}

/**
 * Created by alexgri on 05.03.15.
 */
class ConfiguredDBs(val config: Config) extends TypesafeConfig
with DBs
with TypesafeConfigReader
with NoEnvPrefix
