package io.angstrom.hiveworker.service.api

import io.angstrom.hiveworker.service.Logging

trait NotificationService extends Logging {
  def sendNotification(subject: String, message: String)
}
