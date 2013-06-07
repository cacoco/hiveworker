import io.angstrom.hiveworker.service.api.{JobFlow, JobFlowConfiguration}
import io.angstrom.hiveworker.util.{JobType, Step, StepArgument}

new JobFlowConfiguration {
  override val jobs: Set[JobFlow] = Set[JobFlow](
    JobFlow(
      `type` = JobType.HOURLY,
      script = "hourly_impression_job.q",
      name = Some("hourly_job"),
      visibleToAllUsers = Some(true),
      instances = Some(4),
      maxAttempts = Some(5),
      Step("foo", "bar"),
      Step("LAST_HOUR", StepArgument.LastHour),
      Step("TODAY", StepArgument.Today),
      Step("HH", StepArgument.Hour)),
    JobFlow(
      `type` = JobType.DAILY,
      script = "daily_impression_job.q",
      name = Some("daily_job"),
      visibleToAllUsers = Some(true),
      instances = Some(8),
      maxAttempts = Some(5),
      Step("YESTERDAY", StepArgument.Yesterday),
      Step("TODAY", StepArgument.Today),
      Step("HH", StepArgument.Hour))
  )
}