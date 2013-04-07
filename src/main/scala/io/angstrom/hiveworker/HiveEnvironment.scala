package io.angstrom.hiveworker

trait HiveEnvironment {
  def hadoopVersion: String
  def amiVersion: String
  def hiveVersion: String
  def nodeHeapSize: String
}

case class HiveEnvironmentImpl(
  hadoopVersion: String,
  amiVersion: String,
  hiveVersion: String,
  nodeHeapSize: String
) extends HiveEnvironment

object DefaultHiveEnvironment extends HiveEnvironment {
  val _hadoopVersion: String = "0.20.205"
  def hadoopVersion: String = _hadoopVersion
  val _amiVersion: String = "2.0.4"
  def amiVersion: String = _amiVersion
  val _hiveVersion: String = "0.7.1.3"
  def hiveVersion: String = _hiveVersion
  val _nodeHeapSize: String = "2048"
  def nodeHeapSize: String = _nodeHeapSize
}
