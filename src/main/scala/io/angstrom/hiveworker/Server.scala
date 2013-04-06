package io.angstrom.hiveworker

import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.builder.{ServerBuilder, Server => FinagleServer}
import com.twitter.finagle.http.{Http, RichHttp, Response, Request}
import com.twitter.finagle.stats.OstrichStatsReceiver
import com.twitter.logging.Logger
import com.twitter.ostrich.admin.{Service => OstrichService}
import io.angstrom.hiveworker.configuration.ServicesConfiguration
import io.angstrom.hiveworker.filters.HandleExceptionsFilter
import io.angstrom.hiveworker.service.impl.HiveWorkerServiceImpl
import java.net.InetSocketAddress
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.FunctionalConfigApplicationContext

class Server(config: Config) extends OstrichService {
  require(config != null, "Config must be specified")

  val port = config.port.value
  val name = config.name.value

  val log = Logger.get(getClass)

  var server: Option[FinagleServer] = None
  var context: Option[ApplicationContext] = None

  // Don't initialize until after mixed in by another class
  lazy val handleExceptions = new HandleExceptionsFilter
  lazy val respond = new HiveWorkerServiceImpl(context)
  lazy val service: Service[Request, Response] = handleExceptions andThen respond

  lazy val serverSpec = ServerBuilder()
    .codec(RichHttp[Request](Http()))
    .bindTo(new InetSocketAddress(port))
    .name(name)
    .reportTo(new OstrichStatsReceiver)

  override def start() {
//    context = Some(
//      new ClassPathXmlApplicationContext("classpath:hiveworker-context.xml"))
//    context map { _.asInstanceOf[AbstractApplicationContext].registerShutdownHook() }
    context = Some(FunctionalConfigApplicationContext(classOf[ServicesConfiguration]))

    server = Some(serverSpec.build(service))
  }

  override def shutdown() {
    log.debug("Shutdown requested")
    server match {
      case None =>
        log.warning("Server not started, refusing to shutdown")
      case Some(server) =>
        try {
          server.close(0.seconds)
          log.info("Shutdown complete")
        } catch {
          case e: Exception =>
            log.error(e, "Error shutting down server %s listening on port %d", name, port)
        }
    } // server match
  }

  override def reload() {
    log.info("Reload requested, doing nothing but I could re-read the config or something")
  }
}
