package io.angstrom.hiveworker.service.api

import com.twitter.util.Future
import io.angstrom.hiveworker.service.Logging

trait NotificationService extends Logging {

  def sendNotification(subject: String, message: String): Future[Unit]
}
