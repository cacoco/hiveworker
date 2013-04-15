package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, SendMessageRequest}
import com.twitter.logging.Logger
import io.angstrom.hiveworker.service.api.QueueService

class QueueServiceImpl(
  val amazonSQSClient: AmazonSQS,
  val defaultQueueUrl: String
) extends QueueService {
  val log = Logger.get(getClass)

  def getQueueUrl: String = defaultQueueUrl

  def sendMessage(message: String) {
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

  def deleteMessage(receiptHandle: String) {
    val request = new DeleteMessageRequest().
      withQueueUrl(this.getQueueUrl).
      withReceiptHandle(receiptHandle)
    try {
      amazonSQSClient.deleteMessage(request)
    } catch {
      case e: Exception =>
        log.error(e, e.getMessage)
    }
  }
}
