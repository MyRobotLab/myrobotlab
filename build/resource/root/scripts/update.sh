#!/bin/sh

# actual start script - this does not work directly from 
# the file manager (nautilus) so if anyone has an idea
# as to why I'd love to know - 
 
APPDIR="$(dirname -- "${0}")"

cd $APPDIR

# for pre-Java wildcard jar loading  
for LIB in \
    libraries/jar/*.jar \
    ;
do
    CLASSPATH="${CLASSPATH}:${APPDIR}/${LIB}"
done
export CLASSPATH

# Mac's don't use LD_LIBRARY_PATH yet its 
# required to load shared objects on Linux systems
LD_LIBRARY_PATH=`pwd`/libraries/native/x86.32.linux:`pwd`/libraries/native/x86.64.linux:${LD_LIBRARY_PATH}
export LD_LIBRARY_PATH

# export PATH="${APPDIR}/java/bin:${PATH}"
# exporting PATH will not work LD_LIBRARY_PATH is necessary (at least in Fedora)


# java -d32  force 32 bit 
# -classpath ":myrobotlab.jar:./lib/*"  - make note : on Linux ; on Windows ! 
# -Djava.library.path=./bin - can not change or modify LD_LIBRARY_PATH after jvm starts 
# LD_LIBRARY_PATH needed by Linux systems
# -Djava.library.path= needed by mac
# The shell itself need 
# org.myrobotlab.service.Invoker -service Invoker services GUIService gui > log.txt 

java -classpath "./libraries/jar/*" -Djava.library.path="./libraries/native/x86.32.linux:./libraries/native/x86.64.linux:./libraries/native/x86.32.mac" org.myrobotlab.service.Runtime -update -logLevel INFO -logToConsole

