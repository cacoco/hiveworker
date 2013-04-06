package io.angstrom.hiveworker.configuration

import org.springframework.scala.context.function.FunctionalConfiguration
import io.angstrom.hiveworker.service.impl.{JobFlowServiceImpl, NotificationServiceImpl}
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce

class ServicesConfiguration extends FunctionalConfiguration {
  importXml("classpath:/hiveworker-context.xml")

  bean("jobFlowService") {
    new JobFlowServiceImpl(
      getBean[AmazonElasticMapReduce]("elasticMapReduce"),
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
}