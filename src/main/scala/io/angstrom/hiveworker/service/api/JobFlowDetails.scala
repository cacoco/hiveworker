package io.angstrom.hiveworker.service.api

import com.amazonaws.services.elasticmapreduce.model.JobFlowDetail

case class JobFlowDetails(details: Seq[JobFlowDetail] = Seq[JobFlowDetail]())