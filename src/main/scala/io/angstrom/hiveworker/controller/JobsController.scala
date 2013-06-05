package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.server.util.JsonConverter
import com.twitter.util.{Eval, Future}
import io.angstrom.hiveworker.service.api.JobFlowConfiguration
import java.io.File
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext

class JobsController(
applicationContext: Option[ApplicationContext],
jobConfigurationFile: Option[String]) extends Service[HttpRequest, HttpResponse] {

  def apply(request: HttpRequest): Future[HttpResponse] = {
    try {
      val jobFlowConfiguration = (new Eval).apply[JobFlowConfiguration](new File(jobConfigurationFile.get))()
      Future.value(JsonConverter(jobFlowConfiguration))
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
    }
  }

}
