package io.angstrom.hiveworker.configuration

import io.angstrom.hiveworker.{HiveEnvironmentImpl, HiveEnvironment}

// See: http://docs.amazonwebservices.com/ElasticMapReduce/latest/DeveloperGuide/UsingEMR_Hive.html
case class HiveEnvironmentConfig(
  hadoopVersion: String,
  amiVersion: String,
  hiveVersion: String,
  nodeHeapSize: String
) extends (() => HiveEnvironment) {

  def apply(): HiveEnvironment = {
    HiveEnvironmentImpl(hadoopVersion, amiVersion, hiveVersion, nodeHeapSize)
  }
}
