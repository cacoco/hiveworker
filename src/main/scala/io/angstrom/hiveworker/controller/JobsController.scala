package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.server.util.JsonConverter
import com.twitter.util.Future
import io.angstrom.hiveworker.service.api.JobFlowConfiguration
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext

class JobsController(applicationContext: Option[ApplicationContext]) extends Service[HttpRequest, HttpResponse] {

  lazy val jobFlowConfiguration: Option[JobFlowConfiguration] =
    applicationContext map { _.getBean("jobFlowConfiguration").asInstanceOf[JobFlowConfiguration] }

  def apply(request: HttpRequest): Future[HttpResponse] = {
    jobFlowConfiguration match {
      case Some(config) =>
        Future.value(JsonConverter(config()))
      case _ =>
        Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
    }
  }
}
