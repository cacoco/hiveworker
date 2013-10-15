package io.angstrom.hiveworker.service.api

import com.twitter.util.Future
import io.angstrom.hiveworker.service.Logging

trait QueueService extends Logging {

  def getQueueUrl: String

  def sendMessage(message: String): Future[Unit]

  def deleteMessage(receiptHandle: String): Future[Unit]
}
