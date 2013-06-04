package io.angstrom.hiveworker.service.api

trait JobFlowConfiguration {

  val jobs: Set[JobFlow] = Set[JobFlow]()

  def apply(): Set[JobFlow] = jobs
}
