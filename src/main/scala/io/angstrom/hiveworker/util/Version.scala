package io.angstrom.hiveworker.util

import com.twitter.logging.Logger
import java.util.Properties

object Version {
  val log = Logger.get(getClass)

  var build = ""
  var version = ""
  var timestamp = ""

  val inputStream = getClass.getClassLoader.getResourceAsStream("version.properties")
  try {
    val properties = new Properties
    properties.load(inputStream)

    version = properties.getProperty("build.version")
    build = properties.getProperty("build.revision")
    timestamp = properties.getProperty("build.timestamp")
  } finally {
    inputStream.close()
  }
}
