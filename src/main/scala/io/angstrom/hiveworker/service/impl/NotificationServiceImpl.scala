package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.util.json.{JSONException, JSONObject}
import com.twitter.util.{FuturePool, Future}
import io.angstrom.hiveworker.service.api.NotificationService
import javax.inject.{Named, Inject}

class NotificationServiceImpl @Inject()(
  amazonSNSClient: AmazonSNS,
  @Named("aws.sns.topic.arn.job.errors") defaultTopicARN: String) extends NotificationService {

  private lazy val futurePool = FuturePool.unboundedPool

  def sendNotification(subject: String, message: String): Future[Unit] = {
    futurePool {
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
}
