package io.angstrom.hiveworker.service.impl

import com.twitter.server.util.JsonConverter
import com.twitter.util.{Eval, Future}
import io.angstrom.hiveworker.HiveEnvironment
import io.angstrom.hiveworker.service.api._
import io.angstrom.hiveworker.util.Version
import java.io.File
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext

class HiveWorkerServiceImpl(
  contextOption: Option[ApplicationContext],
  hiveEnvironment: Option[HiveEnvironment],
  jobConfigurationFile: Option[String]
) extends HiveWorkerService {
  lazy val notificationService: Option[NotificationService] =
    contextOption map { _.getBean("notificationService").asInstanceOf[NotificationService] }

  lazy val queueService: Option[QueueService] =
    contextOption map { _.getBean("queueService").asInstanceOf[QueueService] }

  lazy val jobFlowService: Option[JobFlowService] =
    for {context <- contextOption
         service = context.getBean("jobFlowService").asInstanceOf[JobFlowService] } yield {
      service.hiveEnvironment_=(hiveEnvironment)
      service
    }

  def apply(request: HttpRequest): Future[HttpResponse] = {
    println(Version.build)
    println(Version.version)
    println(Version.timestamp)

    println(notificationService)
    println(queueService)
    println(jobFlowService)

    val hiveEnvironment =  jobFlowService flatMap { service =>
      service.hiveEnvironment
    }

    try {
      val jobFlowConfiguration = (new Eval).apply[JobFlowConfiguration](new File(jobConfigurationFile.get))
      println(jobFlowConfiguration())
    } catch {
      case e: Throwable => e.printStackTrace()
    }

    Future.value(JsonConverter(hiveEnvironment.get))
  }
}
