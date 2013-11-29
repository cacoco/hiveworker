package io.angstrom.hiveworker.service.api

import com.twitter.util.Future

trait QueueService {

  def getQueueUrl: String

  def sendMessage(message: String): Future[Unit]

  def deleteMessage(receiptHandle: String): Future[Unit]
}
