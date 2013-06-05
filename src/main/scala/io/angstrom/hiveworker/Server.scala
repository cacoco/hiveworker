package io.angstrom.hiveworker

import com.twitter.finagle.http.HttpMuxer
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.angstrom.hiveworker.configuration.ServicesConfiguration
import io.angstrom.hiveworker.controller.{JobsController, MainController}
import io.angstrom.hiveworker.filters.HandleExceptionsFilter
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.FunctionalConfigApplicationContext

object Server extends TwitterServer {

  val contextPropertiesPath = flag("configuration", "", "Path to context properties file.")

  lazy val context: Option[ApplicationContext] = Some(FunctionalConfigApplicationContext(classOf[ServicesConfiguration]))
  lazy val handleExceptions = new HandleExceptionsFilter

  def main() {
    System.setProperty("hiveworker.configuration", contextPropertiesPath.apply())

    HttpMuxer.addHandler("/jobs", (handleExceptions andThen new JobsController(context)))
    val service = handleExceptions andThen new MainController(context)
    for (route <- MainController.routes) {
      HttpMuxer.addHandler(route, service)
    }

    // Grab the Quatrz Scheduler instance from the Factory
    val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler
    // and start it off
    scheduler.start();

    onExit {
      scheduler.shutdown()
      httpServer.close()
    }
    Await.ready(httpServer)
  }
}
