package io.angstrom.hiveworker

import com.twitter.logging.Logger
import com.twitter.ostrich.admin.{ServiceTracker, RuntimeEnvironment}

object Main {
  val log = Logger.get(getClass)

  def main(args: Array[String]) {
    val runtime = RuntimeEnvironment(this, args)
    val server = runtime.loadRuntimeConfig[Server]()
    try {
      log.info("Starting service")
      server.start()
    } catch {
      case e: Throwable =>
        log.error(e, "Failed starting service, exiting")
        ServiceTracker.shutdown()
        System.exit(1)
    }
  }
}
