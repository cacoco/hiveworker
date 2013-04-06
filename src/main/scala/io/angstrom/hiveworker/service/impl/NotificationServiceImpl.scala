package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.sns.AmazonSNS
import io.angstrom.hiveworker.service.api.NotificationService

class NotificationServiceImpl(val amazonSNS: AmazonSNS, val defaultTopicARN: String) extends NotificationService {

  def sendNotification(subject: String, message: String) {}
}
