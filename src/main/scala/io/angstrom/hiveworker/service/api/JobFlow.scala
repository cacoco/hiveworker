package io.angstrom.hiveworker.service.api

import io.angstrom.hiveworker.util.{JobType, Step}
import org.joda.time.{DateTimeZone, DateTime}

case class JobFlow(
  `type`: JobType,
  script: String,
  name: Option[String],
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
