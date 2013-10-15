package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, SendMessageRequest}
import com.twitter.util.{Future, FuturePool}
import io.angstrom.hiveworker.service.api.QueueService
import javax.inject.{Named, Inject}

class QueueServiceImpl @Inject()(
  amazonSQSClient: AmazonSQS,
  @Named("aws.sqs.queue.url.default") defaultQueueUrl: String) extends QueueService {

  private lazy val futurePool = FuturePool.unboundedPool

  def getQueueUrl: String = defaultQueueUrl

  def sendMessage(message: String): Future[Unit] =  {
    futurePool {
      try {
        val request = new SendMessageRequest().
          withMessageBody(message).
          withQueueUrl(this.defaultQueueUrl).
          withDelaySeconds(0)

        Option(amazonSQSClient.sendMessage(request)) map { result =>
          log.info("Sent queue message: [%s]".format(result.getMessageId))
        }
      } catch {
        case e: Exception =>
          log.error(e, e.getMessage)
      }
    }
  }

  def deleteMessage(receiptHandle: String): Future[Unit] = {
    val request = new DeleteMessageRequest().
      withQueueUrl(this.getQueueUrl).
      withReceiptHandle(receiptHandle)
    futurePool {
      try {
        amazonSQSClient.deleteMessage(request)
      } catch {
        case e: Exception =>
          log.error(e, e.getMessage)
      }
    }
  }
}
