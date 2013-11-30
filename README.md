#Hive Worker
![Hive Worker](http://www.gvsu.edu/gvnow/files/photos/EE23917D-D2F3-6596-4A71E2C7851753DA.jpg "Hive Worker")

[![Build Status](https://travis-ci.org/cacoco/hiveworker.png?branch=master)](https://travis-ci.org/cacoco/hiveworker)

Hive Worker is a Scala library to run [Hive](http://hive.apache.org/) job flows on the [Amazon Elastic MapReduce](http://aws.amazon.com/elasticmapreduce/) platform using the [AWS Java SDK](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html).

Hive Worker uses Google [guice](https://code.google.com/p/google-guice/) and [twitter-server](https://github.com/twitter/twitter-server) as a server stack. twitter-server is built on top of [Finagle](https://github.com/twitter/finagle) -- see Finagle's [User's Guide](http://twitter.github.io/finagle/guide/) for more information.

_NOTE_: this library is a work in-progress

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

## Configuration ################################################################

The parsing of the job configuration steps supports a basic form of date/time formatting if the value sent is of type ```io.angstrom.hiveworker.util.StepArgument```. The default timezone is __UTC__ and
is not configurable. Supported formatting includes:

```
Hour, LastHour, Today, Yesterday, TwoDaysAgo, LastMonth
```

Hive Worker uses the [joda-time](http://joda-time.sourceforge.net/) library for date/time manipulation and formatting.

## Running ######################################################################

To run locally:

```
mvn exec:java -Dexec.args="-aws.access.key=ACCESSS_KEY 
-aws.access.secret.key=SECRET_KEY 
-hadoop.bucket=s3:///hadoop.angstrom.io 
-hadoop.log.uri=s3://hadoop.angstrom.io/logs 
-aws.sns.topic.arn.job.errors=arn:aws:sns:us-east-1:111111111111:job-errors 
-aws.sqs.queue.url.default=https://queue.amazonaws.com/11111111111/HIVE_JOB_FLOW"
```