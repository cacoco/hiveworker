package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce
import com.amazonaws.services.elasticmapreduce.model._
import com.amazonaws.services.elasticmapreduce.util.StepFactory
import com.twitter.util._
import io.angstrom.hiveworker.HiveEnvironment
import io.angstrom.hiveworker.service.api.JobFlow
import io.angstrom.hiveworker.service.api.JobFlowService
import io.angstrom.hiveworker.service.api.SubmitJobFlowResult
import java.util.Date
import scala.collection.JavaConverters._

object JobFlowServiceImpl {
  val MessageKeyJobName = "name"
  val MessageKeyScript = "script"
  val MessageKeyInstanceCount = "instances"
  val MessageKeyStepArgs = "stepArgs"
  val MessageKeyJobFlowId = "jobFlowId"
  val MessageKeyStartTime = "startTime"
  val MessageKeyAttempt = "attempt"

  val Bucket = "bucket"

  def apply(elasticMapReduce: AmazonElasticMapReduce,
            hiveEnvironment: HiveEnvironment,
            defaultJobActionOnFailure: ActionOnFailure,
            bucket: String,
            logUri: String,
            masterInstanceType: String,
            slaveInstanceType: String,
            futurePool: FuturePool = FuturePool.unboundedPool) = {
    new JobFlowServiceImpl(
      elasticMapReduce,
      hiveEnvironment,
      defaultJobActionOnFailure,
      bucket,
      logUri,
      masterInstanceType,
      slaveInstanceType,
      futurePool)
  }
}

class JobFlowServiceImpl(
  val elasticMapReduce: AmazonElasticMapReduce,
  val hiveEnvironment: HiveEnvironment,
  val defaultJobActionOnFailure: ActionOnFailure,
  val bucket: String,
  val logUri: String,
  val masterInstanceType: String,
  val slaveInstanceType: String,
  futurePool: FuturePool) extends JobFlowService {

  import JobFlowServiceImpl._

  /* Public */

  def submitJobFlow(attempt: Integer, jobFlow: JobFlow): Future[Try[SubmitJobFlowResult]] = {
    debug("Submitting job flow task for script: [%s], attempt: [%s]".format(jobFlow.script, attempt))

    val maxAttempts: Int = jobFlow.maxAttempts getOrElse 1
    if (attempt > maxAttempts) {
      return Future.exception(new Exception("Max attempts: %s exceeded.".format(maxAttempts)))
    }
    val request = createJobFlowRequest(jobFlow)
    futurePool {
      runJobFlow(request) map SubmitJobFlowResult
    }
  }

  def describeJobFlow(jobFlowId: String): Future[Try[JobFlowDetail]] = {
    debug("Describing job flow with id: " + jobFlowId)
    val request = new DescribeJobFlowsRequest().
      withJobFlowIds(Seq(jobFlowId).asJavaCollection)
    futurePool {
      Try(elasticMapReduce.describeJobFlows(request)) map { _.getJobFlows.get(0) } // pop off first (and only) result.
    }
  }

  def describeJobFlows(
    createdAfter: Option[Date],
    createdBefore: Option[Date],
    jobFlowIds: Seq[String],
    jobFlowStates: Seq[String]): Future[Try[Seq[JobFlowDetail]]] = {
    val request = new DescribeJobFlowsRequest().
      withJobFlowIds(jobFlowIds.asJavaCollection).
      withJobFlowStates(jobFlowStates: _*)

    createdAfter map request.withCreatedAfter
    createdBefore map request.withCreatedBefore
    debug("Describing job flows with criteria: " + request)
    futurePool {
      Try(elasticMapReduce.describeJobFlows(request)) map { _.getJobFlows.asScala.toSeq }
    }
  }

  def terminateJobFlows(jobFlowIds: String*): Future[Unit] = {
    debug("Terminating job flows: " + jobFlowIds.mkString(","))

    val request = new TerminateJobFlowsRequest().withJobFlowIds(jobFlowIds:_ *)
    futurePool {
      elasticMapReduce.terminateJobFlows(request)
    }
  }

  /* Private */

  private[impl] def createJobFlowRequest(jobFlowConfiguration: JobFlow): RunJobFlowRequest = {
    debug("Creating job flow request for configuration: " + jobFlowConfiguration.name)
    val name = jobFlowConfiguration.canonicalName

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
      .withActionOnFailure(defaultJobActionOnFailure)
    // Make sure to add the bucket as an step argument
    val arguments = jobFlowConfiguration.steps.foldLeft(List("-d", "%s=%s".format(Bucket, bucket))) { (list, step) =>
      list ++ List("-d", "%s=%s".format(step.name, step.value))
    }
    __runHiveScript.withHadoopJarStep {
      stepFactory.newRunHiveScriptStep(path, arguments.asJavaCollection.toArray(new Array[String](arguments.size)): _*)
    }

    val bootstrapActions: List[BootstrapActionConfig] = List(__configureDaemons)
    val __steps: List[StepConfig] = List(__enableDebugging, __installHive, __runHiveScript)

    val visibleToAllUsers = jobFlowConfiguration.visibleToAllUsers getOrElse false
    // Create JobFlowRequest
    val jobFlowRequest = new RunJobFlowRequest().
      withName(name).
      withVisibleToAllUsers(visibleToAllUsers).
      withAmiVersion(hiveEnvironment.amiVersion).
      withBootstrapActions(bootstrapActions.asJavaCollection).
      withSteps(__steps.asJavaCollection).
      withLogUri(logUri).
      withInstances(getJobFlowInstancesConfig(jobFlowConfiguration.instances, hiveEnvironment.hadoopVersion))

    debug(String.format("RunJobFlowRequest: %s", jobFlowRequest.toString))
    jobFlowRequest
  }

  private[impl] def getJobFlowInstancesConfig(
    instanceCountOverride: Option[Int],
    hadoopVersion: String
  ): JobFlowInstancesConfig = {
    val instances = instanceCountOverride getOrElse 1
    new JobFlowInstancesConfig().
      withInstanceCount(java.lang.Integer.valueOf(instances.toInt)).
      withHadoopVersion(hadoopVersion).
      withMasterInstanceType(masterInstanceType).
      withSlaveInstanceType(slaveInstanceType)
  }

  private[impl] def runJobFlow(request: RunJobFlowRequest): Try[String] = {
    Try(elasticMapReduce.runJobFlow(request)) map { _.getJobFlowId }
  }
}
