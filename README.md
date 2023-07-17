myrobotlab
==========
![WebXR and InMoov2 Simulator](./doc/inmoov-quest2.gif)
## MyRobotLab Nixie Release

Open Source Framework for Robotics and Creative Machine Control
  *You know, for robots!*

* Project Website http://myrobotlab.org 
* Project Discord https://discord.gg/AfScp5x8r5
* Latest Build    [Nixie 1.1.(Latest)](http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/artifact/target/myrobotlab.zip)
* Latest Javadocs [Javdocs](http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/artifact/target/site/apidocs/org/myrobotlab/service/package-summary.html)

## Base Requirements

You will need Java 11 or newer.  If you are only running MyRobotLab you need the JRE (Java Runtime Environment.)  If you are going to be building from source, you'll need the JDK (Java Development Kit) Oracle or OpenJDK will work

## Download the myrobotlab.jar
Download

latest [Nixie 1.1.X](http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/artifact/target/myrobotlab.zip)

stable [Manticore 1.0.2693](https://github.com/MyRobotLab/myrobotlab/releases/tag/1.0.2693)

## Running MyRobotLab

After downloading and unzipping myrobtlab.zip into a new folder, start the appropriate script 
### Linux or MacOS
`myrobotlab.sh`
### Windows
`myrobotlab.bat`

The first time the script runs it will automatically install all services and their dependencies, then it will launch myrobotlab.
The subsequent starting of myrobotlab will skip the installation stage.  If a browser does not automatically start you
can go to http://localhost:8888 to see the web user interface.

## Building Project
MyRobotLab core is written in Java, it is a maven project - Any IDE which can load maven should work.  Its web ui is written in AngularJs and html.  
A few services (e.g. InMoov2 & ProgramAB) are in a different repo.  The can be developed seperately so 3 build instruction sets are described.

All development environments require git and cloning the source.

#### Cloning Repo

create a directory to clone the repositories in  (assuming you're on windows and cloning to the c:\dev directory)

```dos
mkdir c:\dev
cd dev
git clone https://github.com/MyRobotLab/myrobotlab.git
cd c:\dev\myrobotlab
```

### Java Core
If you want to be making core changes, you will need to install a 
Java developement environment

#### Install Java 11
https://www.oracle.com/java/technologies/downloads/#java11

### Building with Eclipse
Download Eclipse for Java Developers At:
https://www.eclipse.org/downloads/packages/


### Building with Maven

MyRobotLab builds using the Apache Maven java build system.

Download Maven At:
https://maven.apache.org/download.cgi
 
To compile and build a myrobotlab.jar  first : ensure that "mvn" (maven version 3.3+ is installed and in the path)

`mvn clean install`  

This should produce a local build for you "myrobotlab.jar" file will be located at

myrobotlab/target/myrobotlab.jar   

If you want to compile and skip the tests, you can use the standard maven approach 

`mvn clean install -DskipTests`

## Web UI Style Guide
* [Title Caps for field names and elements](https://learn.microsoft.com/en-us/previous-versions/windows/desktop/bb246428(v=vs.85)?redirectedfrom=MSDN)
* [No semi-colons for field names if labels exist](https://ux.stackexchange.com/questions/3611/should-label-and-field-be-separated-with-colon)


## Contributing

All development is done on the `develop` branch.  To contribute code, the typical approach is to create an issue about the feature/bug you're working on.

From Github create a branch based off the "develop" branch with a descriptive name  (and associated Issue number if available)
Locally switch to the new branch 
Make code changes
Commit & Push (frequently!)
When code is ready, submit a pull request to the develop branch!
Enjoy the code review, address issues and concern in the code review
Reviewer merges pull request to develop.
Reviewer deletes branch.


The following config should be useful to work directly on WebGui UI and
InMoov2 UI if the repos are checked out at the same level
```yml
!!org.myrobotlab.service.config.WebGuiConfig
autoStartBrowser: false
enableMdns: false
listeners: null
peers: null
port: 8888
resources:
# - ./resource/WebGui/app
# - ./resource
- ./src/main/resources/resource/WebGui/app
- ./src/main/resources/resource
- ./src/main/resources/resource/InMoov2/peers/WebGui/app
type: WebGui
```