#!/bin/sh

# Mac's don't use LD_LIBRARY_PATH yet its 
# required to load shared objects on Linux systems
LD_LIBRARY_PATH=`pwd`/libraries/native/armv6.hfp.32.linux:`pwd`/libraries/native/x86.32.linux:`pwd`/libraries/native/x86.64.linux:${LD_LIBRARY_PATH}
export LD_LIBRARY_PATH

DYLD_LIBRARY_PATH=`pwd`/libraries/native/x86.32.mac:`pwd`/libraries/native/x86.64.mac:${DYLD_LIBRARY_PATH}
export DYLD_LIBRARY_PATH

# java -d32  force 32 bit 
# -classpath ":myrobotlab.jar:./lib/*"  - make note : on Linux ; on Windows ! 
# -Djava.library.path=./bin - can not change or modify LD_LIBRARY_PATH after jvm starts 
# LD_LIBRARY_PATH needed by Linux systems
# -Djava.library.path= needed by mac

java -classpath "./libraries/jar/*:./libraries/jar/x86.32.linux/*:./libraries/jar/x86.64.linux/*:" -Djava.library.path="./libraries/native/armv6.hfp.32.linux:./libraries/native/x86.32.linux:./libraries/native/x86.64.linux:./libraries/native/x86.32.mac:./libraries/native/x86.64.mac" org.myrobotlab.service.Runtime -service webgui WebGUI python Python 

