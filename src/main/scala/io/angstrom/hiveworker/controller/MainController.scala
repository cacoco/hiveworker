package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.logging.Logger
import com.twitter.util.Future
import io.angstrom.hiveworker.service.api.{JobFlowDetails, JobFlowService, QueueService, NotificationService}
import io.angstrom.hiveworker.util.JsonConverter
import java.util.Date
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

object MainController {
  val routes = Set(
    "/status"
  )
}

class MainController(applicationContext: Option[ApplicationContext])
  extends Service[HttpRequest, HttpResponse] {

  lazy val log = Logger(getClass.getSimpleName)

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
    import scala.concurrent.duration._

    request.getUri match {
      case "/status" =>
        jobFlowService match {
          case Some(service) =>
            val futureTry = service.describeJobFlows(createdBefore = Some(new Date())) map { result =>
              result match {
                case Success(v) ⇒
                  JobFlowDetails(v)
                case Failure(e) ⇒
                  log.error(e, e.getMessage)
                  JobFlowDetails()
              }
            }
            // TODO: convert Scala Future to Twitter Future correctly
            val details: Map[String, Any] = Map("status" -> Await.result[JobFlowDetails](futureTry, 30.seconds))
            Future.value(JsonConverter(details))
          case _ =>
            Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
        }
      case _ => Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST))
    }
  }
}
