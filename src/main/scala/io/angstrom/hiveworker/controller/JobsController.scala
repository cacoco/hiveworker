package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.util.Future
import io.angstrom.hiveworker.service.api.JobFlowConfiguration
import io.angstrom.hiveworker.util.JsonConverter
import javax.inject.Inject
import org.jboss.netty.handler.codec.http._

class JobsController @Inject()(
  jobFlowConfiguration: JobFlowConfiguration) extends Service[HttpRequest, HttpResponse] {

  def apply(request: HttpRequest): Future[HttpResponse] = {
    val data = for (job <- jobFlowConfiguration.jobs) yield job
    Future.value(JsonConverter(Map("jobs" -> data)))
  }
}
