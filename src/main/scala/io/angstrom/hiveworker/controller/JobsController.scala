package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.util.Future
import io.angstrom.hiveworker.service.api.{JobFlow, JobFlowConfiguration}
import io.angstrom.hiveworker.util.{JsonConverter, Step}
import org.jboss.netty.handler.codec.http._
import org.springframework.context.ApplicationContext

class JobsController(applicationContext: Option[ApplicationContext]) extends Service[HttpRequest, HttpResponse] {

  lazy val jobFlowConfiguration: Option[JobFlowConfiguration] =
    applicationContext map { _.getBean("jobFlowConfiguration").asInstanceOf[JobFlowConfiguration] }

  def apply(request: HttpRequest): Future[HttpResponse] = {
    jobFlowConfiguration match {
      case Some(config) =>
        val data = config().foldLeft(Seq[Map[String, Any]]()){ (s: Seq[Map[String, Any]], jobFlowConfig: JobFlow) =>
          val steps = jobFlowConfig.steps.foldLeft(Seq[(String, String)]()){ (s: Seq[(String, String)], step: Step) =>
            s :+ (step.name -> step.value)
          }
          val m = Map[String, Any](
            "name" -> jobFlowConfig.name.getOrElse(""),
            "canonical_name" -> jobFlowConfig.canonicalName,
            "type" -> jobFlowConfig.`type`.s,
            "instances" -> jobFlowConfig.instances.getOrElse("0"),
            "max_attempts" -> jobFlowConfig.maxAttempts.getOrElse("0"),
            "script" -> jobFlowConfig.script,
            "steps" -> steps)
          s :+ m
        }

        val jobs: Map[String, Any] = Map("jobs" -> data)
        Future.value(JsonConverter(jobs))
      case _ =>
        Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
    }
  }
}
