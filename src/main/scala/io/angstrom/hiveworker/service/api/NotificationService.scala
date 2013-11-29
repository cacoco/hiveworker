package io.angstrom.hiveworker.service.api

import com.twitter.util.Future

trait NotificationService {

  def sendNotification(subject: String, message: String): Future[Unit]
}
