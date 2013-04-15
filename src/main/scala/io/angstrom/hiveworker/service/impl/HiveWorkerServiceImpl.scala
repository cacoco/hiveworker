package io.angstrom.hiveworker.service.impl

import com.twitter.finagle.Service
import com.twitter.util.Future
import io.angstrom.hiveworker.HiveEnvironment
import io.angstrom.hiveworker.service.api.{QueueService, JobFlowService, NotificationService, HiveWorkerService}
import io.angstrom.hiveworker.util.Version
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext

class HiveWorkerServiceImpl(
  contextOption: Option[ApplicationContext],
  hiveEnvironment: Option[HiveEnvironment],
  jobConfigurationFile: Option[String]
) extends Service[HttpRequest, HttpResponse] with HiveWorkerService {
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

    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    Future.value(response)
  }
}
