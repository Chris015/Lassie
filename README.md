# Lassie

Lassie is an automatic tagging service for AWS resources. It works by downloading log the files from your s3 bucket and extracting the user name of the account which started the resource.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

The program requires that you have set up logging in AWS and that log files for the currently running resources exists.

And IAM user  with the following policy: see policy.txt

A configuration file: see configuration.yaml

A logging properties file. See logging.properties

Lassie uses maven to build and package a shaded jar, which can be run on your local machine.

### Installing

clone the project
cd into the project folder
package with Maven using this command; mvn package
create a configuration.yaml file in the same directory as the jar. There is an example configuration file in the readme which you can use.
create a logging properties file in the same directory as the jar. There is an example properties file in the readme which you can use.
run the jar with the following command
```
 java -jar lassie-1.0-SNAPSHOT-shaded.jar
```

Examples
The following command will fetch logs from one day back
```
java -jar lassie-1.0-SNAPSHOT-shaded.jar
```

The following command will fetch logs from the 25 of September to the current date
```
java -jar lassie-1.0-SNAPSHOT-shaded.jar 2017-09-25 
```

The following command will fetch logs from the current date and three days back
```
java -jar lassie-1.0-SNAPSHOT-shaded.jar 3 
```


## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Dropwizard](http://www.dropwizard.io/1.0.2/docs/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [ROME](https://rometools.github.io/rome/) - Used to generate RSS Feeds

## Authors

* **Christopher Olsson**
* **Markus Averheim**
