package io.angstrom.hiveworker

import com.twitter.app.App
import com.twitter.finagle.Service
import com.twitter.finagle.http.HttpMuxer
import com.twitter.logging.Logging
import com.twitter.server.{HttpServer, Admin}
import com.twitter.util.Await
import io.angstrom.hiveworker.configuration.{HiveEnvironmentConfig, ServicesConfiguration}
import io.angstrom.hiveworker.filters.HandleExceptionsFilter
import io.angstrom.hiveworker.service.impl.HiveWorkerServiceImpl
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.FunctionalConfigApplicationContext

object Server extends App
  with Admin
  with HttpServer
  with Logging {

  val contextPropertiesPath = flag("configuration", "", "Path to context properties file.")
  val jobFile = flag("jobs", "", "Path to job configurations")
  val hiveEnvironmentConfig = HiveEnvironmentConfig(
    hadoopVersion= "0.20.205",
    amiVersion = "2.0.4",
    hiveVersion = "0.7.1.3",
    nodeHeapSize = "2048"
  )

  lazy val context: Option[ApplicationContext] = Some(FunctionalConfigApplicationContext(classOf[ServicesConfiguration]))
  // Don't initialize until after mixed in by another class
  lazy val handleExceptions = new HandleExceptionsFilter
  lazy val respond = new HiveWorkerServiceImpl(context, Some(hiveEnvironmentConfig()), jobFile.get)
  lazy val service: Service[HttpRequest, HttpResponse] = handleExceptions andThen respond

  def main() {
    System.setProperty("hiveworker.configuration", contextPropertiesPath.apply())
    HttpMuxer.addHandler("/", service)
    Await.ready(httpServer)
  }
}
