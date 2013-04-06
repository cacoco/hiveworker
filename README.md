#Hive Worker
![Hive Worker](http://www.gvsu.edu/gvnow/files/photos/EE23917D-D2F3-6596-4A71E2C7851753DA.jpg "Hive Worker")

hiveworker is a Scala library to run [Hive](http://hive.apache.org/) job flows on the [Amazon Elastic MapReduce](http://aws.amazon.com/elasticmapreduce/) platform using the [AWS Java SDK](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html).

## Install ######################################################################

```
git clone git://github.com/cacoco/hiveworker.git
```

## Building ######################################################################

hiveworker is built using [Maven](http://maven.apache.org) and requires Scala 2.10.1

To build, just run:

```
cd hiveworker
mvn clean install
```

## Running ######################################################################

hiveworker uses [Finagle](https://github.com/twitter/finagle) as a server stack. See Finagle's [User's Guide](http://twitter.github.io/finagle/guide/) for more information.

To run:

```
mvn scala:run
```