package io.angstrom.hiveworker.service.api

import com.amazonaws.services.elasticmapreduce.model.JobFlowDetail
import com.twitter.util.{Throw, Return, Try, Future}
import io.angstrom.hiveworker.service.Logging
import java.util.Date

trait JobFlowService extends Logging {

  def submitJobFlow(attempt: Integer, jobFlow: JobFlow): Future[Try[SubmitJobFlowResult]]

  def describeJobFlow(jobFlowId: String): Future[Try[JobFlowDetail]]

  def describeJobFlows(
    createdAfter: Option[Date] = None,
    createdBefore: Option[Date] = None,
    jobFlowIds: Seq[String] = Seq[String](),
    jobFlowStates: Seq[String] = Seq[String]()
  ): Future[Try[Seq[JobFlowDetail]]]

  def terminateJobFlows(jobFlowId: String*)

  def parseJobFlowDetails(results: Try[Seq[JobFlowDetail]]): JobFlowDetails = {
    results match {
      case Return(v: Seq[JobFlowDetail]) ⇒
        JobFlowDetails(details = v)
      case Throw(e) ⇒
        log.error(e, e.getMessage)
        JobFlowDetails()
    }
  }
}
