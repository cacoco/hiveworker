package io.angstrom.hiveworker

import com.twitter.ostrich.admin.config.ServerConfig
import com.twitter.ostrich.admin.RuntimeEnvironment
import com.twitter.logging.config.{ConsoleHandlerConfig, LoggerConfig}
import com.twitter.logging.Logger

class Config extends ServerConfig[Server] {

  var port = required[Int]
  var name = required[String]

  val logger = new LoggerConfig {
    node = ""
    level = Logger.INFO
    handlers = new ConsoleHandlerConfig
  }

  def apply(runtime: RuntimeEnvironment): Server = {
    Logger.configure(logger)
    new Server(this)
  }
}
