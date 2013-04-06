package io.angstrom.hiveworker.filters

import com.twitter.finagle.http.{Response, Request}
import com.twitter.finagle.{Service, SimpleFilter}
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.util.CharsetUtil.UTF_8

class HandleExceptionsFilter extends SimpleFilter[Request, Response] {
  def apply(request: Request, service: Service[Request, Response]) = {
    // `handle` asynchronously handles exceptions.
    service(request) handle { case error =>
      val statusCode = error match {
        case _: IllegalArgumentException =>
          FORBIDDEN
        case _ =>
          INTERNAL_SERVER_ERROR
      }
      val errorResponse = new DefaultHttpResponse(HTTP_1_1, statusCode)
      errorResponse.setContent(copiedBuffer(error.getStackTraceString, UTF_8))

      Response(errorResponse)
    }
  }
}
