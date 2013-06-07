package io.angstrom.hiveworker.util

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}

object StepArgument extends Enumeration {
  type StepArgument = Value
  val Hour, LastHour, Today, Yesterday, TwoDaysAgo, LastMonth = Value
}

/**
 * A utility object for holding Job step configuration for a given job flow.
 * This supports some basic datetime formatting if the value sent is of type: StepArgument.
 * The timezone is UTC by default and is not configurable.
 */
object Step {

  import StepArgument._

  val HourPattern = DateTimeFormat.forPattern("yyyy-MM-dd-HH")
  val DayPattern = DateTimeFormat.forPattern("yyyy-MM-dd")
  val MonthPattern = DateTimeFormat.forPattern("yyyy-MM")


  def apply(name: String, value: Any): Step = {
    val now = DateTime.now(DateTimeZone.UTC)
    val v = value match {
      case s: String => s
      case a: StepArgument =>
        a match {
          case Hour =>
            HourPattern.print(now)
          case LastHour =>
            HourPattern.print(now.minusHours(1))
          case Today =>
            DayPattern.print(now)
          case Yesterday =>
            DayPattern.print(now.minusDays(1))
          case TwoDaysAgo =>
            DayPattern.print(now.minusDays(2))
          case LastMonth =>
            MonthPattern.print(now.minusMonths(1))
          case _ => throw new IllegalArgumentException
        }
      case _ => throw new IllegalArgumentException
    }

    new Step(name, v)
  }
}

protected[hiveworker] class Step(val name: String, val value: String)
