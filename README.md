myrobotlab
==========

Open Source Java Framework for Robotics and Creative Machine Control


## Dependencies

This project depends on another project which has the many libraries needed to compile MRL.
* https://github.com/MyRobotLab/repo

## Building Project

MyRobotLab builds (currently) by using the java build tool called  "ant"  and the dependency manager "ivy" 

Download ant & ivy from  
http://ant.apache.org/bindownload.cgi
http://ant.apache.org/ivy/download.cgi

(Locally, I'm using ivy version 2.2.0  and ant version 1.8.2 , but other/newer versions shoudl work.

Unzip the apache and ant downloads and add the "bin" directories to your path (to make it easier)


Next, clone the myrobotlab and the repo projects.

create a directory to clone the repositories in
"mkdir c:\dev"
"cd dev"
"git clone https://github.com/MyRobotLab/myrobotlab.git'
"git clone https://github.com/MyRobotLab/repo.git"

Now, 

cd "c:\dev\myrobotlab"

run 

"ant dist"  


This should produce a build for you.  the new "myrobotlab.jar" file will be located at

myrobotlab/dist/lib/myrobotlab.jar   




