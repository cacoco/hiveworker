package io.angstrom.hiveworker

import com.twitter.finagle.Service
import com.twitter.finagle.http.HttpMuxer
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.angstrom.hiveworker.controller.{JobsController, MainController}
import io.angstrom.hiveworker.filters.HandleExceptionsFilter
import io.angstrom.hiveworker.processor.{DailyProcessor, HourlyProcessor}
import io.angstrom.hiveworker.util.QuartzScheduler
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import com.google.inject.{Injector, Guice}
import io.angstrom.hiveworker.service.{PropertiesModule, ServicesModule}

object Server extends TwitterServer {
  val flags = Seq(
    flag("aws.access.key", "", ""),
    flag("aws.access.secret.key", "", "")
  )

  val contextPropertiesPath = flag("configuration", "file://./config/hiveworker.properties", "Path to context properties file.")
  val dryRun = flag("dry.run", false, "Don't schedule jobs")

  lazy val handleExceptions = new HandleExceptionsFilter

  /* Mutable state */
  var injector: Injector = null

  protected[Server] def service(controller: Service[HttpRequest, HttpResponse]) = {
    handleExceptions andThen controller
  }

  premain {
    injector = Guice.createInjector(
      PropertiesModule.create(flags),
      ServicesModule)
  }

  postmain {

  }

  def main() {
    import net.codingwell.scalaguice.InjectorExtensions._

    val mainController = injector.instance[MainController]
    val jobsController = injector.instance[JobsController]

    HttpMuxer.addHandler("/jobs", service(jobsController))
    for (route <- MainController.routes) {
      HttpMuxer.addHandler(route, mainController)
    }

    val hourlyProcessor = injector.instance[HourlyProcessor]
    val dailyProcessor = injector.instance[DailyProcessor]

    QuartzScheduler.start()
    if (dryRun()) {
      log.info("Skipping job scheduling. ")
    } else {
      // Create jobs
      QuartzScheduler.schedule("hourly", hourlyProcessor) at "0 0 0/1 1/1 * ? *"
      QuartzScheduler.schedule("daily", dailyProcessor) at "0 0 12 1/1 * ? *"
    }

    onExit {
      QuartzScheduler.stop()
    }
    Await.ready(adminHttpServer)
  }
}
