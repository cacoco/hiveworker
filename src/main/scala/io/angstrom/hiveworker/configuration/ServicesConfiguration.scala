package io.angstrom.hiveworker.configuration

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQS
import com.twitter.util.Eval
import io.angstrom.hiveworker.HiveEnvironment
import io.angstrom.hiveworker.service.api.JobFlowConfiguration
import io.angstrom.hiveworker.service.impl.{QueueServiceImpl, JobFlowServiceImpl, NotificationServiceImpl}
import java.io.File
import org.springframework.scala.context.function.FunctionalConfiguration

class ServicesConfiguration extends FunctionalConfiguration {
  importXml("classpath:/hiveworker-context.xml")

  bean("jobFlowService") {
    new JobFlowServiceImpl(
      getBean[AmazonElasticMapReduce]("elasticMapReduce"),
      getBean[HiveEnvironment]("hiveEnvironment"),
      getBean[String]("bucket"),
      getBean[String]("logUri"),
      getBean[String]("masterInstanceType"),
      getBean[String]("slaveInstanceType"))
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

  bean("jobFlowConfiguration") {
    // this will error if it fails to evaluate properly
    (new Eval).apply[JobFlowConfiguration](new File(getBean[String]("jobConfigurationFile")))
  }
}