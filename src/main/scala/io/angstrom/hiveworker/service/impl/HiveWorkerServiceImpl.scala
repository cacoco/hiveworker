package io.angstrom.hiveworker.service.impl

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Response, Request}
import com.twitter.util.Future
import io.angstrom.hiveworker.HiveEnvironment
import io.angstrom.hiveworker.service.api.{JobFlowService, NotificationService, HiveWorkerService}
import io.angstrom.hiveworker.util.Version
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.springframework.context.ApplicationContext

class HiveWorkerServiceImpl(
  contextOption: Option[ApplicationContext],
  hiveEnvironment: Option[HiveEnvironment]
) extends Service[Request, Response] with HiveWorkerService {
  lazy val notificationService: Option[NotificationService] =
    contextOption map { _.getBean("notificationService").asInstanceOf[NotificationService] }
  lazy val jobFlowService: Option[JobFlowService] =
    for {context <- contextOption
         service = context.getBean("jobFlowService").asInstanceOf[JobFlowService] } yield {
      service.hiveEnvironment_=(hiveEnvironment)
      service
    }

  def apply(request: Request) = {
    println(Version.build)
    println(Version.version)
    println(Version.timestamp)

    println(notificationService)
    println(jobFlowService)

    val response = Response(new DefaultHttpResponse(HTTP_1_1, OK))
    Future.value(response)
  }
}
