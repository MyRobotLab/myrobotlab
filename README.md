myrobotlab
==========

Open Source Java Framework for Robotics and Creative Machine Control


## Dependencies

Dependencies for MyRobotLab are downloaded when you run "java -jar myrobotlab.jar -install"
They are managed via integration with Apache Ivy.

When doing development with MyRobotLab, you can import it as a Maven project and dependencies are managed through maven.
Each service defines a static method called getMetaData().  This method is used to generate the pom.xml file and also to create the .myrobotlab/serviceData.json file which is used by Ivy.

## Building Project

MyRobotLab builds using the Apache Maven java build system.

Download Maven At:
https://maven.apache.org/download.cgi
 
Clone the myrobotlab project

create a directory to clone the repositories in
"mkdir c:\dev"
"cd dev"
"git clone https://github.com/MyRobotLab/myrobotlab.git'
cd "c:\dev\myrobotlab"

run 

"mvn clean install"  

This should produce a local build for you "myrobotlab.jar" file will be located at

myrobotlab/target/myrobotlab.jar   

