package io.angstrom.hiveworker.util

abstract sealed class JobType(val s: String)

object JobType {
  case object HOURLY extends JobType("hourly")
  case object DAILY extends JobType("daily")
}
