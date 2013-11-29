package io.angstrom.hiveworker.service.impl

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce
import com.amazonaws.services.elasticmapreduce.model.{RunJobFlowResult, RunJobFlowRequest, ActionOnFailure}
import com.twitter.util.Await
import io.angstrom.hiveworker.DefaultHiveEnvironment
import io.angstrom.hiveworker.service.api.{SubmitJobFlowResult, JobFlow}
import io.angstrom.hiveworker.util.{StepArgument, Step, JobType}
import org.apache.commons.lang.RandomStringUtils
import org.easymock.EasyMock.anyObject
import org.scalatest.mock.EasyMockSugar
import org.scalatest.{Assertions, ShouldMatchers}
import org.testng.annotations.{Test, AfterTest, BeforeTest}

class JobFlowServiceImplTest
  extends Assertions
  with ShouldMatchers
  with EasyMockSugar {

  /* Invariants */
  val hiveEnvironment = DefaultHiveEnvironment
  val actionOnFailure = ActionOnFailure.TERMINATE_JOB_FLOW
  val bucket = "s3://hadoop.angstrom.io"
  val logUri = "s3://hadoop.angstrom.io/logs"
  val masterInstanceType = "m1.small"
  val slaveInstanceType = "m1.small"

  @BeforeTest(alwaysRun = true)
  def beforeTest() {
    println("in before.")
  }

  @AfterTest(alwaysRun = true)
  def afterTest() {
    println("in after.")
  }

  @Test def testSubmitJobFlow() {
    println("in test.")

    /* mocks */
    val mapReduceClient = mock[AmazonElasticMapReduce]
    implicit val mocks = MockObjects(mapReduceClient)

    val jobFlowId = "j-" + RandomStringUtils.randomAlphabetic(13).toUpperCase
    val runJobFlowResult = new RunJobFlowResult().withJobFlowId(jobFlowId)
    expecting {
      mapReduceClient.runJobFlow(anyObject().asInstanceOf[RunJobFlowRequest]).andReturn(runJobFlowResult)
    }

    val service = JobFlowServiceImpl(
      mapReduceClient,
      hiveEnvironment,
      actionOnFailure,
      bucket,
      logUri,
      masterInstanceType,
      slaveInstanceType)

    val jobFlow = JobFlow(
      JobType.HOURLY,
      "hourly_impressions",
      Some("Hourly Impressions"),
      Some(true),
      Some(1),
      Some(1),
      Step("LAST_HOUR", StepArgument.LastHour))

    whenExecuting {
      val result = Await.result(service.submitJobFlow(1, jobFlow)).get
      result should be(SubmitJobFlowResult(jobFlowId))
    }
  }
}
