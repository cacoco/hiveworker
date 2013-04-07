package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce
import com.amazonaws.services.elasticmapreduce.model._
import com.amazonaws.services.elasticmapreduce.util.StepFactory
import com.twitter.logging.Logger
import io.angstrom.hiveworker.configuration.JobFlowConfiguration
import io.angstrom.hiveworker.service.api.{SubmitJobFlowResult, JobFlowService}
import io.angstrom.hiveworker.util.Step
import io.angstrom.hiveworker.{DefaultHiveEnvironment, HiveEnvironment}
import java.util.Date
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, future}
import scala.util.{Success, Failure, Try}

object JobFlowServiceImpl {
  val MessageKeyJobName = "name"
  val MessageKeyScript = "script"
  val MessageKeyInstanceCount = "instances"
  val MessageKeyStepArgs = "stepArgs"
  val MessageKeyJobFlowId = "jobFlowId"
  val MessageKeyStartTime = "startTime"
  val MessageKeyAttempt = "attempt"
}

class JobFlowServiceImpl(
  val elasticMapReduceClient: AmazonElasticMapReduce,
  val bucket: String,
  val logUri: String,
  val masterInstanceType: String,
  val slaveInstanceType: String
) extends JobFlowService {
  val log = Logger.get(getClass)

  private[this] var hiveEnvironmentOption: Option[HiveEnvironment] = None
  def hiveEnvironment: Option[HiveEnvironment] = hiveEnvironmentOption
  def hiveEnvironment_= (hiveEnvironment: Option[HiveEnvironment]) {
    this.hiveEnvironmentOption = hiveEnvironment
  }

  def submitJobFlow(attempt: Integer, jobFlowConfiguration: JobFlowConfiguration): Future[Try[SubmitJobFlowResult]] = {
    log.info("Creating job flow task for script: [%s], attempt: [%s]".format(jobFlowConfiguration.script, attempt))

    val maxAttempts: Integer = jobFlowConfiguration.maxAttempts getOrElse 1
    if (attempt > maxAttempts) {
      return Future(Failure(new Exception("Max attempts: %s exceeded.".format(maxAttempts))))
    }
    val request = createJobFlowRequest(jobFlowConfiguration)
    future {
      runJobFlow(request) match {
        case Success(jobFlowId) =>
          Success(SubmitJobFlowResult(jobFlowId))
        case _ =>
          Failure(new Exception("Job flow id is null. Request: [%s] attempt: [%s].".format(request.getName, attempt)))
      }
    }
  }

  protected[this] def createJobFlowRequest(jobFlowConfiguration: JobFlowConfiguration): RunJobFlowRequest = {
    val hiveEnvironment = hiveEnvironmentOption getOrElse DefaultHiveEnvironment

    val name = jobFlowConfiguration.name getOrElse {
        // use script name with a timestamp
        val script = jobFlowConfiguration.script.replaceAll(".q", "")
        "%s %s".format(script, System.currentTimeMillis())
    }

    val __configureDaemons = new BootstrapActionConfig()
    __configureDaemons.setName("configure_daemons_bootstrap_action")

    // we'll be running a bootstrap script, setup configuration
    val configureDaemonsScriptBootstrapAction = new ScriptBootstrapActionConfig
    configureDaemonsScriptBootstrapAction.setPath("s3://elasticmapreduce/bootstrap-actions/configure-daemons")
    val args = Seq(
      "--namenode-heap-size=%s".format(hiveEnvironment.nodeHeapSize),
      "--datanode-heap-size=%s".format(hiveEnvironment.nodeHeapSize)
    )
    configureDaemonsScriptBootstrapAction.setArgs(args.asJavaCollection)
    __configureDaemons.setScriptBootstrapAction(configureDaemonsScriptBootstrapAction)

    // CONFIGURE STEPS: enable debugging, setup hive, install hive site and run hive script
    val stepFactory = new StepFactory

    val __enableDebugging = new StepConfig()
      .withName("Enable Debugging")
      .withActionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW)
      .withHadoopJarStep(stepFactory.newEnableDebuggingStep())

    val __installHive = new StepConfig()
      .withName("Setup/Install Hive")
      .withActionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW)
      .withHadoopJarStep(stepFactory.newInstallHiveStep(hiveEnvironment.hiveVersion))

    val path = "%1$s/scripts/%2$s".format(bucket, jobFlowConfiguration.script)
    val __runHiveScript = new StepConfig()
      .withName(String.format("Run Hive Script"))
      .withActionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW) // TODO: is TERMINATE correct?
    // Make sure to add the bucket as an step argument
    val arguments = jobFlowConfiguration.steps.foldLeft(List("-d", "%s=%s".format(Step.Bucket, bucket))) { (list, step) =>
      list ++ List("-d", "%s=%s".format(step.name, step.value))
    }
    __runHiveScript.withHadoopJarStep {
      stepFactory.newRunHiveScriptStep(path, arguments.asJavaCollection.toArray(new Array[String](arguments.size)): _*)
    }

    val bootstrapActions: List[BootstrapActionConfig] = List(__configureDaemons)
    val __steps: List[StepConfig] = List(__enableDebugging, __installHive, __runHiveScript)

    // Create JobFlowRequest
    val jobFlowRequest = new RunJobFlowRequest().
      withName(name).
      withAmiVersion(hiveEnvironment.amiVersion).
      withBootstrapActions(bootstrapActions.asJavaCollection).
      withSteps(__steps.asJavaCollection).
      withLogUri(logUri).
      withInstances(getJobFlowInstancesConfig(jobFlowConfiguration.instances, hiveEnvironment.hadoopVersion))

    log.debug(String.format("RunJobFlowRequest: %s", jobFlowRequest.toString))
    jobFlowRequest
  }

  protected[this] def getJobFlowInstancesConfig(
    instanceCountOverride: Option[Integer],
    hadoopVersion: String
  ): JobFlowInstancesConfig = {
    new JobFlowInstancesConfig().
        withInstanceCount(instanceCountOverride getOrElse 1).
        withHadoopVersion(hadoopVersion).
        withMasterInstanceType(masterInstanceType).
        withSlaveInstanceType(slaveInstanceType)
  }

  protected[this] def runJobFlow(request: RunJobFlowRequest): Try[String] = {
    for (runJobFlowResult <- Try(elasticMapReduceClient.runJobFlow(request)))
      yield runJobFlowResult.getJobFlowId
  }

  def describeJobFlow(jobFlowId: String): Future[Try[JobFlowDetail]] = {
    val request = new DescribeJobFlowsRequest().
      withJobFlowIds(Seq(jobFlowId).asJavaCollection)
    future {
      for (describeResult <- Try(elasticMapReduceClient.describeJobFlows(request)))
        yield describeResult.getJobFlows.get(0) // pop off first (and only) result.s
    }
  }

  def describeJobFlows(
    createdAfter: Date,
    createdBefore: Date,
    jobFlowIds: Seq[String],
    jobFlowStates: String*
  ): Future[Try[Seq[JobFlowDetail]]] = {
    val request = new DescribeJobFlowsRequest().
      withCreatedAfter(createdAfter).
      withCreatedBefore(createdBefore).
      withJobFlowIds(jobFlowIds.asJavaCollection).
      withJobFlowStates(jobFlowStates: _*)
    future {
      for (describeResult <- Try(elasticMapReduceClient.describeJobFlows(request)))
        yield describeResult.getJobFlows.asScala.toSeq
    }
  }

  def terminateJobFlows(jobFlowIds: String*) {
    val request = new TerminateJobFlowsRequest().withJobFlowIds(jobFlowIds:_ *)
    future {
      elasticMapReduceClient.terminateJobFlows(request)
    }
  }
}
