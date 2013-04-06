package io.angstrom.hiveworker.configuration

import io.angstrom.hiveworker.util.Step

case class JobFlowConfiguration(script: String, name: Option[String], instances: Option[Integer], maxAttempts: Option[Integer], steps: Step*)
