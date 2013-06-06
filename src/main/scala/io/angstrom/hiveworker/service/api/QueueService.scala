package io.angstrom.hiveworker.service.api

import io.angstrom.hiveworker.service.Logging

trait QueueService extends Logging {

  def getQueueUrl: String

  def sendMessage(message: String)

  def deleteMessage(receiptHandle: String)
}
