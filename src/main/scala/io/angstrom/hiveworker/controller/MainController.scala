package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.logging.Logger
import com.twitter.util.Future
import io.angstrom.hiveworker.service.api.{JobFlowService, QueueService, NotificationService}
import io.angstrom.hiveworker.util.JsonConverter
import java.util.Date
import javax.inject.Inject
import org.jboss.netty.handler.codec.http._

object MainController {
  val routes = Set(
    "/status"
  )
}

class MainController @Inject()(
  notificationService: NotificationService,
  queueService: QueueService,
  jobFlowService: JobFlowService)
  extends Service[HttpRequest, HttpResponse] {

  lazy val log = Logger(getClass.getSimpleName)

  def apply(request: HttpRequest): Future[HttpResponse] = {
    handleRequest(request)
  }

  protected[this] def handleRequest(request: HttpRequest): Future[HttpResponse] = {
    request.getUri match {
      case "/status" =>
        val jobFlowDetailsFuture =
          jobFlowService.describeJobFlows(
            createdBefore = Some(new Date())) map jobFlowService.parseJobFlowDetails
        jobFlowDetailsFuture map { details =>
          JsonConverter(details)
        }
      case _ => Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST))
    }
  }
}
