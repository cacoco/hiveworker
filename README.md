#Hive Worker
![Hive Worker](http://www.gvsu.edu/gvnow/files/photos/EE23917D-D2F3-6596-4A71E2C7851753DA.jpg "Hive Worker")

[![Build Status](https://travis-ci.org/cacoco/hiveworker.png?branch=master)](https://travis-ci.org/cacoco/hiveworker)

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

## Configuration ################################################################

Hive Worker uses [Spring](http://static.springsource.org/spring/docs/current/javadoc-api/) for dependency injection. All you need to do is point Hive Worker to a ```hiveworker.properties``` file.

```
aws.access.key=YOUR_KEY
aws.access.secret.key=YOUR_SECRET
aws.client.connection.timeout=50000
aws.client.max.connections=10
aws.client.socket.timeout=50000
aws.sns.topic.arn.job.errors=arn:aws:sns:us-west-2:111111111111:mapreduce-job-errors
aws.sqs.queue.url.default=https://queue.amazonaws.com/111111111111/HIVE_JOB_FLOW
hadoop.bucket=s3://hadoop.angstrom.io
hadoop.instance.type.master=m1.small
hadoop.instance.type.slave=m1.small
hadoop.log.uri=s3://hadoop.angstrom.io/logs
job.action.onfailure=TERMINATE_JOB_FLOW
jobs.configuration.file=/path/to/job_configuration.scala
```

For the Job configuration file, see ```examples/example_config.scala``` as an example.

The parsing of the job configuration steps supports a basic form of date/time formatting if the value sent is of type ```io.angstrom.hiveworker.util.StepArgument```. The default timezone is __UTC__ and
is not configurable. Supported formatting includes:

```
Hour, LastHour, Today, Yesterday, TwoDaysAgo, LastMonth
```

Hive Worker uses the [joda-time](http://joda-time.sourceforge.net/) library for date/time manipulation and formatting.

## Running ######################################################################

Hive Worker uses [Finagle](https://github.com/twitter/finagle) as a server stack. See Finagle's [User's Guide](http://twitter.github.io/finagle/guide/) for more information.

To run:

```
mvn exec:java -Dexec.args="-configuration=file:/path/to/hiveworker.properties"
```