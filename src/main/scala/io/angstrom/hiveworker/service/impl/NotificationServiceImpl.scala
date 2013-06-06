package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.util.json.{JSONException, JSONObject}
import io.angstrom.hiveworker.service.api.NotificationService

class NotificationServiceImpl(
  val amazonSNSClient: AmazonSNS,
  val defaultTopicARN: String) extends NotificationService {

  def sendNotification(subject: String, message: String) {
    try {
      val json = new JSONObject()
      json.put("default", message)

      val request = new PublishRequest().
        withTopicArn(defaultTopicARN).
        withSubject(subject).
        withMessage(json.toString).
        withMessageStructure("json")

      Option(amazonSNSClient.publish(request)) map { result =>
        log.info("Published notification: [%s]".format(result.getMessageId))
      }
    } catch {
      case e: JSONException =>
        log.error(e, e.getMessage)
      case e: Exception =>
        log.error(e, e.getMessage)
    }
  }
}
