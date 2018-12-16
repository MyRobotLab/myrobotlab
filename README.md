myrobotlab
==========

Open Source Java Framework for Robotics and Creative Machine Control


## Installing Dependencies

Dependencies for MyRobotLab are downloaded when you run "java -jar myrobotlab.jar -install"
They are managed via integration with Apache Ivy.

When doing development with MyRobotLab, you can import it as a Maven project and dependencies are managed through maven.
Each service defines a static method called getMetaData().  This method is used to generate the pom.xml file and also to create the .myrobotlab/serviceData.json file which is used by Ivy.

## Running MyRobotLab

You will need Java 8 or newer.  If you are only running MyRobotLab you need the JRE (Java Runtime Environment.)  If you are going to be building from source, you'll need the JDK (Java Development Kit)

Make sure java is in your path. open a terminal window and change to the directory where you've downloaded the myroblab.jar

To start MyRobotLab you can do so by running

`java -jar myrobotlab.jar`

If you want to start MyRobotLab and execute a python script on startup, you can use the following command:

`java -jar myrobotlab.jar -service runtime Runtime python Python gui SwingGui -invoke python execFile MyScript.py`

To pass additional arguments to the JVM on startup (such as increasing the java memory / heap size you can use the -jvm arg : 

`java -jar myrobotlab.jar -jvm "-Xmx512m -Xms512m"`  

To start a list of named services on startup, you can use the -service command line arg with a list of service name/type pairs.

`java -jar myrobotlab.jar -service runtime Runtime python Python gui SwingGui wegbui WebGui` 

The above would start runtime, python, the swing gui and lastly the webgui.




## Building Project

MyRobotLab builds using the Apache Maven java build system.

Download Maven At:
https://maven.apache.org/download.cgi
 
Clone the myrobotlab project

create a directory to clone the repositories in  (assuming you're on windows and cloning to the c:\dev directory)

`mkdir c:\dev`
`cd dev`
`git clone https://github.com/MyRobotLab/myrobotlab.git`
`cd c:\dev\myrobotlab`

To compile and build a myrobotlab.jar  first : ensure that "mvn" (maven version 3.3+ is installed and in the path)

`mvn clean install`  

This should produce a local build for you "myrobotlab.jar" file will be located at

myrobotlab/target/myrobotlab.jar   

If you want to compile and skip the tests, you can use the standard maven approach 

`mvn clean install -DskipTests`



