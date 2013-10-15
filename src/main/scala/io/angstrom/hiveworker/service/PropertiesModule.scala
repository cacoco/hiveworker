package io.angstrom.hiveworker.service

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.twitter.app.Flag
import scala.collection.JavaConverters._
import scala.collection.breakOut

object PropertiesModule {
  def create(flags: Seq[Flag[_]]) = {
    val propertiesMap = (flags map { elem =>
      elem.name -> elem.apply().toString
    })(breakOut)
    new PropertiesModule(propertiesMap.toMap)
  }
}

class PropertiesModule(properties: Map[String, String]) extends AbstractModule {
  def configure() {
    Names.bindProperties(binder(), properties.asJava)
  }
}
