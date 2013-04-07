package io.angstrom.hiveworker

import com.twitter.logging.{LoggerFactory, Logger}
import com.twitter.ostrich.admin.RuntimeEnvironment
import com.twitter.ostrich.admin.config.ServerConfig
import io.angstrom.hiveworker.configuration.HiveEnvironmentConfig

class Config extends ServerConfig[Server] {

  var port = required[Int]
  var name = required[String]

  var loggerFactory = required[LoggerFactory]
  var hiveEnvironmentConfig = required[HiveEnvironmentConfig]

  def apply(runtime: RuntimeEnvironment): Server = {
    Logger.configure(List(loggerFactory.value))
    new Server(this)
  }
}
