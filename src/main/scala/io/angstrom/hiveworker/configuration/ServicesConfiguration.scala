package io.angstrom.hiveworker.configuration

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQS
import io.angstrom.hiveworker.HiveEnvironment
import io.angstrom.hiveworker.service.impl.{QueueServiceImpl, JobFlowServiceImpl, NotificationServiceImpl}
import org.springframework.scala.context.function.FunctionalConfiguration

class ServicesConfiguration extends FunctionalConfiguration {
  importXml("classpath:/hiveworker-context.xml")

  bean("jobFlowService") {
    val service = new JobFlowServiceImpl(
      getBean[AmazonElasticMapReduce]("elasticMapReduce"),
      getBean[String]("bucket"),
      getBean[String]("logUri"),
      getBean[String]("masterInstanceType"),
      getBean[String]("slaveInstanceType"))
    service.hiveEnvironment = Option(getBean[HiveEnvironment]("hiveEnvironment"))
    service
  }

  bean("notificationService") {
    new NotificationServiceImpl(
      getBean[AmazonSNS]("amazonSNS"),
      getBean[String]("defaultTopicARN"))
  }

  bean("queueService") {
    new QueueServiceImpl(
      getBean[AmazonSQS]("amazonSQS"),
      getBean[String]("defaultQueueUrl"))
  }
}