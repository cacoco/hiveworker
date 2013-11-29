package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce
import com.amazonaws.services.elasticmapreduce.model._
import com.twitter.util.Await
import io.angstrom.hiveworker.DefaultHiveEnvironment
import io.angstrom.hiveworker.service.api.JobFlow
import io.angstrom.hiveworker.service.api.SubmitJobFlowResult
import io.angstrom.hiveworker.util.{StepArgument, Step, JobType}
import org.apache.commons.lang.RandomStringUtils
import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.reset
import org.scalatest.ShouldMatchers
import org.scalatest.mock.EasyMockSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations._
import scala.collection.JavaConverters.asJavaCollectionConverter

class JobFlowServiceImplTest
  extends TestNGSuite
  with ShouldMatchers
  with EasyMockSugar {

  /* Invariants */
  val hiveEnvironment = DefaultHiveEnvironment
  val actionOnFailure = ActionOnFailure.TERMINATE_JOB_FLOW
  val bucket = "s3://hadoop.angstrom.io"
  val logUri = "s3://hadoop.angstrom.io/logs"
  val masterInstanceType = "m1.small"
  val slaveInstanceType = "m1.small"

  /* mocks */
  val mapReduceClient = mock[AmazonElasticMapReduce]
  implicit val mocks = MockObjects(mapReduceClient)

  /* */
  val service = JobFlowServiceImpl(
    mapReduceClient,
    hiveEnvironment,
    actionOnFailure,
    bucket,
    logUri,
    masterInstanceType,
    slaveInstanceType)

  @BeforeTest(alwaysRun = true)
  def beforeTest() {}

  @AfterTest(alwaysRun = true)
  def afterTest() {
    for (mock <- mocks.mocks) {
      reset(mock)
    }
  }

  @Test def testCreateJobFlowRequest() {
    val steps = Seq(
      Step("LAST_HOUR", StepArgument.LastHour),
      Step("TODAY", StepArgument.Today),
      Step("HH", StepArgument.Hour))

    val jobFlow = JobFlow(
      JobType.HOURLY,
      "process_hourly_usage",
      Some("Process Hourly Usage"),
      Some(true),
      Some(1),
      Some(1),
      steps: _*)
    val request = service.createJobFlowRequest(jobFlow)
    request shouldNot be(null)
    request.getName shouldNot be(null)
    request.getSteps.size should be(3) // enable debugging, install and run.
  }

  @Test def testSubmitJobFlow() {
    val jobFlowId = randomJobFlowId()
    val runJobFlowResult = new RunJobFlowResult().withJobFlowId(jobFlowId)
    expecting {
      mapReduceClient.runJobFlow(null).andReturn(runJobFlowResult)
    }

    val steps = Seq(
      Step("LAST_HOUR", StepArgument.LastHour),
      Step("TODAY", StepArgument.Today),
      Step("HH", StepArgument.Hour))

    val jobFlow = JobFlow(
      JobType.HOURLY,
      "process_hourly_usage",
      Some("Process Hourly Usage"),
      Some(true),
      Some(1),
      Some(1),
      steps: _*)

    whenExecuting {
      val result = Await.result(service.submitJobFlow(1, jobFlow)).get()
      result should be(SubmitJobFlowResult(jobFlowId))
    }
  }

  @Test def testDescribeJobFlow() {
    val jobFlowId = randomJobFlowId()
    val result = new DescribeJobFlowsResult().withJobFlows(mockJobFlowDetailsWithIds(jobFlowId).asJavaCollection)
    expecting {
      mapReduceClient.describeJobFlows(anyObject().asInstanceOf[DescribeJobFlowsRequest]).andReturn(result)
    }

    whenExecuting {
      val jobFlowDetail = Await.result(service.describeJobFlow(jobFlowId)).get()
      jobFlowDetail shouldNot be(null)
      jobFlowDetail.getJobFlowId should be(jobFlowId)
    }
  }

  @Test def testDescribeJobFlows() {
    val result = new DescribeJobFlowsResult().withJobFlows(mockJobFlowDetails.asJavaCollection)
    expecting {
      mapReduceClient.describeJobFlows(anyObject().asInstanceOf[DescribeJobFlowsRequest]).andReturn(result)
    }

    whenExecuting {
      val jobFlowDetails = Await.result(service.describeJobFlows()).get()
      jobFlowDetails shouldNot be(null)
      jobFlowDetails.size should be > 0
    }
  }

  /* Test helpers */

  private def mockJobFlowDetailsWithIds(jobFlowIds: String*): Seq[JobFlowDetail] = {
    (for (jobFlowId <- jobFlowIds) yield {
      val executionStatusDetail = new JobFlowExecutionStatusDetail().withState("COMPLETED")
      val instances = new JobFlowInstancesDetail().withInstanceCount(1)
      new JobFlowDetail(
        jobFlowId,
        RandomStringUtils.randomAlphanumeric(10),
        executionStatusDetail,
        instances)
    }).toSeq
  }

  private def mockJobFlowDetails: Seq[JobFlowDetail] = {
    val ids = for (i <- 1 to 3) yield { randomJobFlowId() }
    mockJobFlowDetailsWithIds(ids.toSeq: _*)
  }

  private def randomJobFlowId() = "j-" + RandomStringUtils.randomAlphanumeric(13).toUpperCase
}
