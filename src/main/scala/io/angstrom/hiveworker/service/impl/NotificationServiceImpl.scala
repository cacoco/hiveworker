package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.util.json.{JSONException, JSONObject}
import com.twitter.util.{FuturePool, Future}
import grizzled.slf4j.Logging
import io.angstrom.hiveworker.service.api.NotificationService
import javax.inject.{Named, Inject}

class NotificationServiceImpl @Inject()(
  amazonSNSClient: AmazonSNS,
  @Named("aws.sns.topic.arn.job.errors") defaultTopicARN: String)
  extends NotificationService
  with Logging {

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
          info("Published notification: [%s]".format(result.getMessageId))
        }
      } catch {
        case e: JSONException => error(e.getMessage, e)
        case e: Exception => error(e.getMessage, e)
      }
    }
  }
}
