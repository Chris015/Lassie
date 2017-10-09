# Lassie

Lassie is an automatic tagging service for AWS resources. It works by downloading log the files from your s3 bucket and extracting the user name of the account which started the resource. Lassie is intended to be run from the terminal or as a cronjob.

**The following resources are currently supported:**

* Ec2Instance
* S3Bucket
* EbsVolume
* RdsDbInstance
* RedshiftCluster
* EmrCluster
* LoadBalancer

**Lassie can be run with additional input parameters and system properties:**

**Input parameters**

1. -jar lassie-1.0-SNAPSHOT-shader.jar **date** (Optional, refer to to the "Installing" section for different versions of date)

**System properties**

1.  **-Dlog4j.configurationFile="log4j2.xml"** -jar ... (Required for logging.)
2. **-Dmockmode="true"**.
Optional. This is used for mocking in Integration Tests. If not set, it defaults to false.) 

***Maven-failsafe-plugin will automatically set mockmode to true and the log4j.configurationFile during the integration tests***

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
The files mentioned in this section can be found in the templates folder.

1. The program requires that you have set up logging in AWS.
2. An IAM user  with the following policy: see **policy.txt**
3. A configuration file: see **configuration.yaml**
4. A logging properties file: see **log4j2.xml**

Lassie uses maven to build and package in a shaded JAR, which can be run on your local machine.

### Installing

1. clone the project
2. package with Maven using the following command: **mvn package**
3. create a configuration.yaml file in the same directory as the jar. There is an example configuration file in the templates folder.
4. create a logging properties file in the same directory as the jar. There is an example properties file in the templates folder.
5. run the jar with one of the following commands:

**Examples**

Fetch logs and tag resources from the current date
```
 java -Dlog4j.configurationFile="log4j2.xml" -jar lassie-1.0-SNAPSHOT-shaded.jar 
```

Fetch logs and tag resources from the 25 of September to the current date
```
 java -Dlog4j.configurationFile="log4j2.xml" -jar lassie-1.0-SNAPSHOT-shaded.jar 2017-09-25
```

Fetch logs and tag resources from the current date and three days back
```
 java -Dlog4j.configurationFile="log4j2.xml" -jar lassie-1.0-SNAPSHOT-shaded.jar 3
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Christopher Olsson**
* [**Markus Averheim**](https://github.com/averheim) 
