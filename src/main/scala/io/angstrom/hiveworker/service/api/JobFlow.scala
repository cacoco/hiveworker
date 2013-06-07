package io.angstrom.hiveworker.service.api

import io.angstrom.hiveworker.util.{JobType, Step}
import org.joda.time.{DateTimeZone, DateTime}

/**
 * Holds configuration metadata for an Elastic Map Reduce job flow.
 * @param `type`- one of a value from io.angstrom.hiveworker.util.JobType
 * @param script - Hive script to run.
 * @param name - optional name of job flow. Will be derived from script if None.
 * @param visibleToAllUsers - if this job flow should be visible to all IAM users of the account. Default is false.
 * @param instances - optional number of EC2 instances to use. Default is 1.
 * @param maxAttempts - optional number of attempts for job flow creation. Default is 1.
 * @param steps - optional set of (opaque) io.angstrom.hiveworker.util.Steps to add when creating the job flow.
 */
case class JobFlow(
  `type`: JobType,
  script: String,
  name: Option[String],
  visibleToAllUsers: Option[Boolean],
  instances: Option[Int],
  maxAttempts: Option[Int],
  steps: Step*) {

  def canonicalName = {
    val prefix = name getOrElse script.replaceAll(".q", "")
    val now = DateTime.now(DateTimeZone.UTC)
    val timestamp = `type` match {
      case JobType.HOURLY => Step.HourPattern.print(now)
      case JobType.DAILY => Step.DayPattern.print(now)
      case _ => throw new IllegalArgumentException
    }

    "%s %s".format(prefix, timestamp)
  }
}
