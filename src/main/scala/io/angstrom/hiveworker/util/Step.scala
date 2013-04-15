package io.angstrom.hiveworker.util

import java.text.SimpleDateFormat
import java.util.Date

object Step {
  val format = """\$\{([a-zA-Z]+)\}""".r

  val Bucket = "bucket"

  def apply(name: String, value: String) {
    if (value.startsWith("$")) {
      // handle ${XX} values which are SimpleDateFormat patterns for datetimes
      val parsedValue = value match {
        case format(pattern) =>
          val dateFormat = new SimpleDateFormat(pattern)
          dateFormat.format(new Date())
      }
      new Step(name, parsedValue)
    } else {
      new Step(name, value)
    }
  }
}

class Step(val name: String, val value: String)
