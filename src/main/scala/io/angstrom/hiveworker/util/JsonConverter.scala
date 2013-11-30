package io.angstrom.hiveworker.util

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter
import com.fasterxml.jackson.databind.{PropertyNamingStrategy, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.{HttpResponseStatus, HttpVersion, DefaultHttpResponse, HttpResponse}

object JsonConverter {
  private[this] val writer = {
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
    val printer = new MinimalPrettyPrinter
    mapper.writer(printer)
  }

  def apply(obj: Any): HttpResponse = {
    val msg = writer.writeValueAsString(obj) + "\n"
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    response.setContent(ChannelBuffers.wrappedBuffer(msg.getBytes))
    response
  }
}
