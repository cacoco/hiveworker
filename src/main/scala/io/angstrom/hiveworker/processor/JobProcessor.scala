package io.angstrom.hiveworker.processor

import com.twitter.logging.Logger
import com.twitter.util.Await
import io.angstrom.hiveworker.service.api.{JobFlowDetails, JobFlowService, JobFlowConfiguration}
import io.angstrom.hiveworker.util.JobType
import java.util.Date
import org.quartz.JobExecutionContext

abstract class JobProcessor(
  jobFlowConfiguration: JobFlowConfiguration,
  jobFlowService: JobFlowService) extends (JobExecutionContext => Unit) {

  lazy val log = Logger(getClass.getSimpleName)

  def jobType: JobType

  def apply(context: JobExecutionContext) {
    execute()
  }

  private def execute() {
    val jobs = jobFlowConfiguration() filter { _.`type` == jobType }
    log.debug("configured jobs: " , jobs)

    val jobFlowDetailsFuture = jobFlowService.describeJobFlows(
        createdBefore = Some(new Date())) map jobFlowService.parseJobFlowDetails

    val jobDetails = Await.result[JobFlowDetails](jobFlowDetailsFuture)
    val createdJobs = jobDetails.details map { detail => detail.getName }
    // check to see if we've created this job before -- TODO: look at the state detail to determine most recent execution state.
    val jobsToCreate = jobs filterNot { createdJobs contains _.canonicalName }
    log.debug("jobs to create:", jobsToCreate)

    for (job <- jobsToCreate) {
      jobFlowService.submitJobFlow(1, job)
    }
  }
}
