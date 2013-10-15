package io.angstrom.hiveworker

import com.google.inject.{Injector, Guice}
import com.twitter.finagle.http.HttpMuxer
import com.twitter.finagle.{Filter, Service}
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.TwitterServer
import com.twitter.util.{Future, Await}
import io.angstrom.hiveworker.controller.{JobsController, MainController}
import io.angstrom.hiveworker.filters.HandleExceptionsFilter
import io.angstrom.hiveworker.processor.{DailyProcessor, HourlyProcessor}
import io.angstrom.hiveworker.service.{PropertiesModule, ServicesModule}
import io.angstrom.hiveworker.util.QuartzScheduler
import net.codingwell.scalaguice.InjectorExtensions._
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}

object Server
  extends TwitterServer
  with Warmup {

  /* Flags to bind to named properties */
  val flags = Seq(
    flag("aws.access.key", "FOO", ""),
    flag("aws.access.secret.key", "BAR", ""),
    flag("aws.client.connection.timeout", "50000", ""),
    flag("aws.client.max.connections", "10", ""),
    flag("aws.client.socket.timeout", "50000", ""),
    flag("aws.sns.topic.arn.job.errors", "arn:aws:sns:us-east-1:111111111111:job-errors", ""),
    flag("aws.sqs.queue.url.default", "https://queue.amazonaws.com/11111111111/HIVE_JOB_FLOW", ""),
    flag("hadoop.bucket", "s3://hadoop.angstrom.io", ""),
    flag("hadoop.instance.type.master", "m1.small", ""),
    flag("hadoop.instance.type.slave", "m1.small", ""),
    flag("hadoop.log.uri", "s3://hadoop.angstrom.io/logs", ""),
    flag("job.action.onfailure", "TERMINATE_JOB_FLOW", ""))
  val dryRun = flag("dry.run", false, "Don't schedule jobs")

  /* Mutable state */
  var injector: Injector = _

  premain {
    injector = Guice.createInjector(
      PropertiesModule(flags),
      ServicesModule)

    addRoute[HandleExceptionsFilter, JobsController]("/jobs")
    for (route <- MainController.routes) {
      addRoute[HandleExceptionsFilter, MainController](route)
    }
  }

  /* Handle warmup and server awaiting */
  final def main() {
    Await.result {
      for {
        _ <- warmup()
      } yield { warmupComplete() }
    }
    Await.ready(adminHttpServer)
  }

  /* Handle controller routing */
  type HttpFilter = Filter[HttpRequest, HttpResponse, HttpRequest, HttpResponse]
  type HttpService = Service[HttpRequest, HttpResponse]
  protected def addRoute[FilterType <: HttpFilter : Manifest, ServiceType <: HttpService : Manifest](pattern: String) {
    HttpMuxer.addHandler(pattern, injector.instance[FilterType] andThen injector.instance[ServiceType] )
  }

  /* Wait for Quartz to initialize before server starts */
  protected def warmup(): Future[Unit] = {
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

    Future.Unit
  }
}
