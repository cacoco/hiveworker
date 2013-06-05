package io.angstrom.hiveworker.processor

import com.twitter.logging.Logger
import io.angstrom.hiveworker.service.api.{JobFlowDetails, JobFlowService, JobFlowConfiguration}
import io.angstrom.hiveworker.util.JobType
import java.util.Date
import org.springframework.context.ApplicationContext
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

abstract class JobProcessor(applicationContext: Option[ApplicationContext]) {

  lazy val log = Logger(getClass.getSimpleName)

  lazy val jobFlowConfiguration: Option[JobFlowConfiguration] =
    applicationContext map { _.getBean("jobFlowConfiguration").asInstanceOf[JobFlowConfiguration] }
  lazy val jobFlowService: Option[JobFlowService] =
    applicationContext map { _.getBean("jobFlowService").asInstanceOf[JobFlowService] }

  def jobType: JobType

  def execute() {
    import scala.concurrent.duration._

    for {config <- jobFlowConfiguration
         service <- jobFlowService } {
      val jobs = config() filter { _.`type` == jobType }
      log.info("" , jobs)

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
      val jobDetails = Await.result[JobFlowDetails](futureTry, 30.seconds)
      val createdJobs = jobDetails.details map { detail => detail.getName }
      // check to see if we've created this job before -- TODO: look at the state detail to determine most recent execution state.
      val jobsToCreate = jobs filterNot { createdJobs contains _.canonicalName }

      for (job <- jobsToCreate) {
        service.submitJobFlow(1, job)
      }
    }
  }

}
