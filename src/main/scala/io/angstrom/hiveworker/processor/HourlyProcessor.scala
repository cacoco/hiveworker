package io.angstrom.hiveworker.processor

import io.angstrom.hiveworker.service.api.{JobFlowService, JobFlowConfiguration}
import io.angstrom.hiveworker.util.JobType
import javax.inject.Inject

class HourlyProcessor @Inject()(
  jobFlowConfiguration: JobFlowConfiguration,
  jobFlowService: JobFlowService)
  extends JobProcessor(jobFlowConfiguration, jobFlowService) {
  def jobType = JobType.HOURLY
}
