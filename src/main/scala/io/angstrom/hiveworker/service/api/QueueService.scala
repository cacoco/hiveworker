package io.angstrom.hiveworker.service.api

trait QueueService {

  def getQueueUrl: String

  def sendMessage(message: String)

  def deleteMessage(receiptHandle: String)
}
