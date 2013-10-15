package io.angstrom.hiveworker.service

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentials, BasicAWSCredentials}
import com.amazonaws.services.elasticmapreduce.model.ActionOnFailure
import com.amazonaws.services.elasticmapreduce.{AmazonElasticMapReduce, AmazonElasticMapReduceClient}
import com.google.inject.{Provides, AbstractModule}
import io.angstrom.hiveworker.service.api.JobFlowService
import io.angstrom.hiveworker.service.impl.JobFlowServiceImpl
import io.angstrom.hiveworker.{DefaultHiveEnvironment, HiveEnvironment}
import javax.inject.{Named, Singleton}
import net.codingwell.scalaguice.ScalaModule

object ServicesModule extends AbstractModule with ScalaModule {

  def configure() {}

  @Provides
  @Singleton
  def providesHiveEnvironment(): HiveEnvironment = {
    DefaultHiveEnvironment
  }

  @Provides
  @Singleton
  def providesAWSCredentials(
    @Named("aws.access.key") accessKey: String,
    @Named("aws.access.secret.key") secretKey: String): BasicAWSCredentials = {
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
  def providesJobFlowService(
    elasticMapReduce: AmazonElasticMapReduce,
    hiveEnvironment: HiveEnvironment,
    defaultJobActionOnFailure: ActionOnFailure,
    @Named("hadoop.bucket") bucket: String,
    @Named("hadoop.log.uri") logUri: String,
    @Named("hadoop.instance.type.master") masterInstanceType: String,
    @Named("hadoop.instance.type.slave") slaveInstanceType: String): JobFlowService = {
    new JobFlowServiceImpl(
      elasticMapReduce,
      hiveEnvironment,
      defaultJobActionOnFailure,
      bucket,
      logUri,
      masterInstanceType,
      slaveInstanceType)
  }
}
