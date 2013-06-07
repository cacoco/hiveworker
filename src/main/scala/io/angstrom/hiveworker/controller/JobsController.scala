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
        val data = config().foldLeft(Seq[Map[String, Any]]()){ (s, jobFlow) =>
          s :+ mapJobFlowObject(jobFlow)
        }

        val jobs: Map[String, Any] = Map("jobs" -> data)
        Future.value(JsonConverter(jobs))
      case _ =>
        Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
    }
  }

  private def mapStepObjects(steps: Seq[Step]): Seq[(String, String)] = {
    steps.foldLeft(Seq[(String, String)]()){ (s, step) => s :+ (step.name -> step.value) }
  }

  private def mapJobFlowObject(jobFlow: JobFlow): Map[String, Any] = {
    Map[String, Any](
      "name" -> jobFlow.name.getOrElse(""),
      "canonical_name" -> jobFlow.canonicalName,
      "type" -> jobFlow.`type`.s,
      "instances" -> jobFlow.instances.getOrElse("0"),
      "max_attempts" -> jobFlow.maxAttempts.getOrElse("0"),
      "script" -> jobFlow.script,
      "steps" -> mapStepObjects(jobFlow.steps))
  }
}
