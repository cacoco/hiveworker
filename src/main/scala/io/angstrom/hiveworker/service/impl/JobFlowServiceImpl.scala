package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce
import com.amazonaws.services.elasticmapreduce.model._
import com.amazonaws.services.elasticmapreduce.util.StepFactory
import io.angstrom.hiveworker.HiveEnvironment
import io.angstrom.hiveworker.service.api.{JobFlow, SubmitJobFlowResult, JobFlowService}
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

  val Bucket = "bucket"
}

class JobFlowServiceImpl(
  val elasticMapReduceClient: AmazonElasticMapReduce,
  val hiveEnvironment: HiveEnvironment,
  val bucket: String,
  val logUri: String,
  val masterInstanceType: String,
  val slaveInstanceType: String) extends JobFlowService {

  import JobFlowServiceImpl._

  def submitJobFlow(attempt: Integer, jobFlow: JobFlow): Future[Try[SubmitJobFlowResult]] = {
    log.info("Creating job flow task for script: [%s], attempt: [%s]".format(jobFlow.script, attempt))

    val maxAttempts: Int = jobFlow.maxAttempts getOrElse 1
    if (attempt > maxAttempts) {
      return Future(Failure(new Exception("Max attempts: %s exceeded.".format(maxAttempts))))
    }
    val request = createJobFlowRequest(jobFlow)
    future {
      runJobFlow(request) match {
        case Success(jobFlowId) =>
          Success(SubmitJobFlowResult(jobFlowId))
        case _ =>
          Failure(new Exception("Job flow id is null. Request: [%s] attempt: [%s].".format(request.getName, attempt)))
      }
    }
  }

  protected[this] def createJobFlowRequest(jobFlowConfiguration: JobFlow): RunJobFlowRequest = {
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
      .withActionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW) // TODO: is TERMINATE correct?
    // Make sure to add the bucket as an step argument
    val arguments = jobFlowConfiguration.steps.foldLeft(List("-d", "%s=%s".format(Bucket, bucket))) { (list, step) =>
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

  protected[this] def runJobFlow(request: RunJobFlowRequest): Try[String] = {
    for (runJobFlowResult <- Try(elasticMapReduceClient.runJobFlow(request)))
      yield runJobFlowResult.getJobFlowId
  }

  def describeJobFlow(jobFlowId: String): Future[Try[JobFlowDetail]] = {
    val request = new DescribeJobFlowsRequest().
      withJobFlowIds(Seq(jobFlowId).asJavaCollection)
    future {
      for (describeResult <- Try(elasticMapReduceClient.describeJobFlows(request)))
        yield describeResult.getJobFlows.get(0) // pop off first (and only) result.
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
    createdAfter map { request.withCreatedAfter(_) }
    createdBefore map { request.withCreatedBefore(_) }
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
