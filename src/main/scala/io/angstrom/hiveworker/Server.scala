package io.angstrom.hiveworker

import com.twitter.finagle.http.HttpMuxer
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.angstrom.hiveworker.configuration.ServicesConfiguration
import io.angstrom.hiveworker.controller.{JobsController, MainController}
import io.angstrom.hiveworker.filters.HandleExceptionsFilter
import io.angstrom.hiveworker.processor.{DailyProcessor, HourlyProcessor}
import io.angstrom.hiveworker.util.QuartzScheduler
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.FunctionalConfigApplicationContext

object Server extends TwitterServer {
  val contextPropertiesPath = flag("configuration", "file://./config/hiveworker.properties", "Path to context properties file.")
  val dryRun = flag("dry.run", false, "Don't schedule jobs")

  lazy val context: Option[ApplicationContext] = Some(FunctionalConfigApplicationContext(classOf[ServicesConfiguration]))
  lazy val handleExceptions = new HandleExceptionsFilter

  def main() {
    System.setProperty("hiveworker.configuration", contextPropertiesPath.apply())

    HttpMuxer.addHandler("/jobs", (handleExceptions andThen new JobsController(context)))
    val service = handleExceptions andThen new MainController(context)
    for (route <- MainController.routes) {
      HttpMuxer.addHandler(route, service)
    }

    QuartzScheduler.start()
    if (dryRun()) {
      log.info("Skipping job scheduling. ")
    } else {
      // Create jobs
      QuartzScheduler.schedule("hourly", new HourlyProcessor(context)) at "0 0 0/1 1/1 * ? *"
      QuartzScheduler.schedule("daily", new DailyProcessor(context)) at "0 0 12 1/1 * ? *"
    }

    onExit {
      QuartzScheduler.stop()
      httpServer.close()
    }
    Await.ready(httpServer)
  }
}
