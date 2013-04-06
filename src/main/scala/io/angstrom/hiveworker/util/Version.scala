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

    version = properties.getProperty("version")
    build = properties.getProperty("build")
    timestamp = properties.getProperty("timestamp")
  } finally {
    inputStream.close()
  }
}
