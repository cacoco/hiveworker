package io.angstrom.hiveworker.service.api

import io.angstrom.hiveworker.util.Step

case class JobFlow(script: String, name: Option[String], instances: Option[Int], maxAttempts: Option[Int], steps: Step*)
