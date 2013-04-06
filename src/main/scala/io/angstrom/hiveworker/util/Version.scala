package io.angstrom.hiveworker.util

import java.util.Properties
import com.twitter.logging.Logger

object Version {
  val log = Logger.get(getClass)

  var build = ""
  var version = ""
  var timestamp = ""

  val inputStream = getClass.getClassLoader.getResourceAsStream("version.properties")
  try {
    val properties = new Properties
    properties.load(inputStream)

    version = properties.getProperty("io.angstrom.hiveworker.build.version")
    build = properties.getProperty("io.angstrom.hiveworker.build.revision")
    timestamp = properties.getProperty("io.angstrom.hiveworker.build.timestamp")
  } finally {
    inputStream.close()
  }
}
