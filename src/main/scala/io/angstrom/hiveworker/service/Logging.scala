package io.angstrom.hiveworker.service

import com.twitter.logging.Logger

trait Logging {
  protected lazy val log = Logger.get(getClass.getSimpleName)
}
