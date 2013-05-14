package io.angstrom.hiveworker.util

import com.twitter.logging.Logger
import java.util.Properties

object Version {
  val log = Logger.get(getClass)

  var build = ""
  var version = ""
  var timestamp = ""

  val inputStream = getClass.getClassLoader.getResourceAsStream("io/angstrom/hiveworker/build.properties")
  try {
    val properties = new Properties
    properties.load(inputStream)

    version = properties.getProperty("version")
    build = properties.getProperty("build_revision")
    timestamp = properties.getProperty("timestamp")
  } finally {
    inputStream.close()
  }
}
