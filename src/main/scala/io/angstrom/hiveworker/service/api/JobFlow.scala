package io.angstrom.hiveworker.service.api

import io.angstrom.hiveworker.util.{JobType, Step}
import org.joda.time.DateTime

case class JobFlow(
  `type`: JobType,
  script: String,
  name: Option[String],
  instances: Option[Int],
  maxAttempts: Option[Int],
  steps: Step*) {

  def canonicalName = {
    val prefix = name getOrElse script.replaceAll(".q", "")
    val timestamp = `type` match {
      case JobType.HOURLY => Step.HourPattern.print(DateTime.now)
      case JobType.DAILY => Step.DayPattern.print(DateTime.now)
      case _ => throw new IllegalArgumentException
    }

    "%s %s".format(prefix, timestamp)
  }
}
