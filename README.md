# Lassie

Lassie is an automatic tagging service for AWS resources. It works by downloading log the files from your s3 bucket and extracting the user name of the account which started the resource.

**The following resources are currently supported:**
-Ec2Instance
-S3Bucket
-EbsVolume
-RdsDbInstance
-RedshiftCluster
-EmrCluster
-LoadBalancer

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

The program requires that you have set up logging in AWS and that log files for the currently running resources exists.

And IAM user  with the following policy: see **policy.txt**

A configuration file: see **configuration.yaml**

A logging properties file: see **logging.properties**

Lassie uses maven to build and package a shaded jar, which can be run on your local machine.

### Installing

1. clone the project
2. package with Maven using the following command: **mvn package**
3. create a configuration.yaml file in the same directory as the jar. There is an example configuration file in the readme which you can use
4. create a logging properties file in the same directory as the jar. There is an example properties file in the readme which you can use
5. run the jar with the following command
```
 java -jar lassie-1.0-SNAPSHOT-shaded.jar
```

**Examples**

Fetch logs from current date
```
java -jar lassie-1.0-SNAPSHOT-shaded.jar
```

Fetch logs from the 25 of September to the current date
```
java -jar lassie-1.0-SNAPSHOT-shaded.jar 2017-09-25 
```

Fetch logs from the current date and three days back
```
java -jar lassie-1.0-SNAPSHOT-shaded.jar 3 
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Christopher Olsson**
* **Markus Averheim**
