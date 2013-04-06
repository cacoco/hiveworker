package io.angstrom.hiveworker.service.api

trait NotificationService {
  def sendNotification(subject: String, message: String)
}
