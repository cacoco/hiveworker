package io.angstrom.hiveworker.controller

import com.twitter.finagle.Service
import com.twitter.util.Future
import io.angstrom.hiveworker.service.api.{JobFlow, JobFlowConfiguration}
import io.angstrom.hiveworker.util.{JsonConverter, Step}
import org.jboss.netty.handler.codec.http._
import javax.inject.Inject

class JobsController @Inject()(
  jobFlowConfiguration: JobFlowConfiguration) extends Service[HttpRequest, HttpResponse] {

  def apply(request: HttpRequest): Future[HttpResponse] = {
    val data = jobFlowConfiguration().foldLeft(Seq[Map[String, Any]]()){ (s, jobFlow) =>
      s :+ mapJobFlowObject(jobFlow)
    }

    val jobs: Map[String, Any] = Map("jobs" -> data)
    Future.value(JsonConverter(jobs))
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
