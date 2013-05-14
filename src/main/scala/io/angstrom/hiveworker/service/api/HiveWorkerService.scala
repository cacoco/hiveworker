package io.angstrom.hiveworker.service.api

import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}

trait HiveWorkerService extends Service[HttpRequest, HttpResponse] {

}
