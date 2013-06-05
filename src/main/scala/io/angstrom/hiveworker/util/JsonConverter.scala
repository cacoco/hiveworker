package io.angstrom.hiveworker.util

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.{HttpResponseStatus, HttpVersion, DefaultHttpResponse, HttpResponse}

object JsonConverter {
  private[this] val writer = {
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val printer = new DefaultPrettyPrinter
    printer.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter)
    mapper.writer(printer)
  }

  def apply(obj: Any): HttpResponse = {
    val msg = writer.writeValueAsString(obj) + "\n"
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    response.setContent(ChannelBuffers.wrappedBuffer(msg.getBytes))
    response
  }
}
