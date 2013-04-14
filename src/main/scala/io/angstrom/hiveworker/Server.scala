package io.angstrom.hiveworker

import com.twitter.app._
import com.twitter.finagle.{Http, Service}
import com.twitter.logging.{ConsoleHandler, LoggerFactory, Logger}
import com.twitter.util.Await
import io.angstrom.hiveworker.configuration.{HiveEnvironmentConfig, ServicesConfiguration}
import io.angstrom.hiveworker.filters.HandleExceptionsFilter
import io.angstrom.hiveworker.service.impl.HiveWorkerServiceImpl
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.FunctionalConfigApplicationContext

object Server extends App {

  val port = flag("port", 8080, "Port")
  val contextPropertiesPath = flag("configuration", "", "Path to context properties file.")
  val jobFile = flag("jobs", "", "Path to job configurations")
  val hiveEnvironmentConfig = HiveEnvironmentConfig(
    hadoopVersion= "0.20.205",
    amiVersion = "2.0.4",
    hiveVersion = "0.7.1.3",
    nodeHeapSize = "2048"
  )
  val loggerFactory = LoggerFactory(
    node = "",
    level = Some(Logger.INFO),
    handlers = List(ConsoleHandler())
  )

  val log = Logger.get(getClass)

  var context: Option[ApplicationContext] = None
  // Don't initialize until after mixed in by another class
  lazy val handleExceptions = new HandleExceptionsFilter
  lazy val respond = new HiveWorkerServiceImpl(context, Some(hiveEnvironmentConfig()))
  lazy val service: Service[HttpRequest, HttpResponse] = handleExceptions andThen respond

  def main() {
    // set the System property
    System.setProperty("hiveworker.configuration", contextPropertiesPath.apply())
    context = Some(FunctionalConfigApplicationContext(classOf[ServicesConfiguration]))
    val server = Http.serve(":%s".format(port.apply()), service)
    Await.ready(server)
  }
}
