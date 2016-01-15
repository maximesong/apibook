# Setup
Download install sbt from http://www.scala-sbt.org.

## Setup database
Install MongoDB  form https://www.mongodb.org. Then run scripts/run_mongo.sh

# Run
To invoke APIBook commands, type the following in command line
``sbt core/run <cmd>``

See Makefile for common commands

To start APIBook website, type the following in command line
``sbt core/web``


# Packaging
To generate assembly package for the application, please run
``sbt -mem 2048 "core/assembly"``
and
``sbt -mem 2048 "web/assembly"``

To run application using packaged jar file, just run
``java -jar <core-assembly.jar> <cmd>``
