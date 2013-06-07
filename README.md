#Hive Worker
![Hive Worker](http://www.gvsu.edu/gvnow/files/photos/EE23917D-D2F3-6596-4A71E2C7851753DA.jpg "Hive Worker") [![Build Status](https://travis-ci.org/cacoco/hiveworker.png?branch=master)](https://travis-ci.org/cacoco/hiveworker)

Hive Worker is a Scala library to schedule [Hive](http://hive.apache.org/) job flows on the [Amazon Elastic MapReduce](http://aws.amazon.com/elasticmapreduce/) platform using the [AWS Java SDK](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html).

NOTE: this library is still in-progress

## Install ######################################################################

```
git clone git://github.com/cacoco/hiveworker.git
```

## Building ######################################################################

Hive Worker is built using [Maven](http://maven.apache.org) and requires Scala 2.10.1

To build, just run:

```
cd hiveworker
mvn clean install
```

## Running ######################################################################

Hive Worker uses [Finagle](https://github.com/twitter/finagle) as a server stack. See Finagle's [User's Guide](http://twitter.github.io/finagle/guide/) for more information.

To run:

```
mvn exec:java -Dexec.args="-configuration=file:/path/to/hiveworker.properties -jobs=/path/to/job/configuration.scala"
```