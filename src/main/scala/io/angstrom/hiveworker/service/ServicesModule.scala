package io.angstrom.hiveworker.service

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentials, BasicAWSCredentials}
import com.amazonaws.services.elasticmapreduce.model.ActionOnFailure
import com.amazonaws.services.elasticmapreduce.{AmazonElasticMapReduce, AmazonElasticMapReduceClient}
import com.amazonaws.services.sns.{AmazonSNSClient, AmazonSNS}
import com.amazonaws.services.sqs.{AmazonSQSClient, AmazonSQS}
import com.google.inject.{Provides, AbstractModule}
import io.angstrom.hiveworker.service.api._
import io.angstrom.hiveworker.service.impl.{QueueServiceImpl, NotificationServiceImpl, JobFlowServiceImpl}
import io.angstrom.hiveworker.util.{StepArgument, Step, JobType}
import io.angstrom.hiveworker.{DefaultHiveEnvironment, HiveEnvironment}
import javax.inject.{Named, Singleton}
import net.codingwell.scalaguice.ScalaModule
import scala.Some

object ServicesModule extends AbstractModule with ScalaModule {

  def configure() {
    bind[NotificationService].to[NotificationServiceImpl]
    bind[QueueService].to[QueueServiceImpl]
  }

  @Provides
  @Singleton
  def providesHiveEnvironment(): HiveEnvironment = {
    DefaultHiveEnvironment
  }

  @Provides
  @Singleton
  def providesAWSCredentials(
    @Named("aws.access.key") accessKey: String,
    @Named("aws.access.secret.key") secretKey: String): AWSCredentials = {
    new BasicAWSCredentials(accessKey, secretKey)
  }

  @Provides
  @Singleton
  def providesClientConfiguration(
    @Named("aws.client.socket.timeout") socketTimeout: Int,
    @Named("aws.client.connection.timeout") connectionTimeout: Int,
    @Named("aws.client.max.connections") maxConnections: Int): ClientConfiguration = {
    new ClientConfiguration().
      withSocketTimeout(socketTimeout).
      withConnectionTimeout(connectionTimeout).
      withMaxConnections(maxConnections)
  }

  @Provides
  @Singleton
  def providesElasticMapReduce(
    awsCredentials: AWSCredentials,
    clientConfiguration: ClientConfiguration): AmazonElasticMapReduce = {
    new AmazonElasticMapReduceClient(awsCredentials, clientConfiguration)
  }

  @Provides
  @Singleton
  def providesActionOfFailure(
    @Named("job.action.onfailure") defaultActionOnFailureString: String): ActionOnFailure = {
    ActionOnFailure.valueOf(defaultActionOnFailureString)
  }

  @Provides
  @Singleton
  def providesAmazonSNS(
    awsCredentials: AWSCredentials,
    clientConfiguration: ClientConfiguration): AmazonSNS = {
    new AmazonSNSClient(awsCredentials, clientConfiguration)
  }

  @Provides
  @Singleton
  def providesAmazonSQS(
    awsCredentials: AWSCredentials,
    clientConfiguration: ClientConfiguration): AmazonSQS = {
    new AmazonSQSClient(awsCredentials, clientConfiguration)
  }

  @Provides
  @Singleton
  def providesJobFlowService(
    elasticMapReduce: AmazonElasticMapReduce,
    hiveEnvironment: HiveEnvironment,
    defaultJobActionOnFailure: ActionOnFailure,
    @Named("hadoop.bucket") bucket: String,
    @Named("hadoop.log.uri") logUri: String,
    @Named("hadoop.instance.type.master") masterInstanceType: String,
    @Named("hadoop.instance.type.slave") slaveInstanceType: String): JobFlowService = {
    JobFlowServiceImpl(
      elasticMapReduce,
      hiveEnvironment,
      defaultJobActionOnFailure,
      bucket,
      logUri,
      masterInstanceType,
      slaveInstanceType)
  }

  @Provides
  @Singleton
  def providesJobFlowConfiguration(): JobFlowConfiguration = {
    new JobFlowConfiguration {
      override val jobs: Set[JobFlow] = Set[JobFlow](
        JobFlow(
          `type` = JobType.HOURLY,
          script = "hourly_impression_job.q",
          name = Some("hourly_job"),
          visibleToAllUsers = Some(true),
          instances = Some(4),
          maxAttempts = Some(5),
          Seq(Step("foo", "bar"),
            Step("LAST_HOUR", StepArgument.LastHour),
            Step("TODAY", StepArgument.Today),
            Step("HH", StepArgument.Hour)).toMap),
        JobFlow(
          `type` = JobType.DAILY,
          script = "daily_impression_job.q",
          name = Some("daily_job"),
          visibleToAllUsers = Some(true),
          instances = Some(8),
          maxAttempts = Some(5),
          Seq(Step("YESTERDAY", StepArgument.Yesterday),
            Step("TODAY", StepArgument.Today),
            Step("HH", StepArgument.Hour)).toMap)
      )
    }
  }
}
