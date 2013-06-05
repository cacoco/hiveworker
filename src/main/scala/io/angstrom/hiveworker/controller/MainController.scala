package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.util.Future
import io.angstrom.hiveworker.service.api.{JobFlowService, QueueService, NotificationService}
import java.util.Date
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure }

object MainController {
  val routes = Set(
    "/status"
  )
}

class MainController(applicationContext: Option[ApplicationContext]) extends Service[HttpRequest, HttpResponse] {
  lazy val notificationService: Option[NotificationService] =
    applicationContext map { _.getBean("notificationService").asInstanceOf[NotificationService] }

  lazy val queueService: Option[QueueService] =
    applicationContext map { _.getBean("queueService").asInstanceOf[QueueService] }

  lazy val jobFlowService: Option[JobFlowService] =
    applicationContext map { _.getBean("jobFlowService").asInstanceOf[JobFlowService] }

  def apply(request: HttpRequest): Future[HttpResponse] = {
    handleRequest(request)
  }

  protected[this] def handleRequest(request: HttpRequest): Future[HttpResponse] = {
    request.getUri match {
      case "/status" =>
        jobFlowService map { service =>
          service.describeJobFlows(createdBefore = Some(new Date())) map { result =>
            result match {
              // TODO:
              case Success(v) ⇒ println(v)
              case Failure(_) ⇒ println("failed")
            }
          }
        }

        Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
      case _ => Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST))
    }
  }
}
