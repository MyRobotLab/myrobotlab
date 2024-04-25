#!/usr/bin/env bash

REPO_FILE=libraries/repo.json

APPDIR="$(dirname -- ${0})"

echo APPDIR=${APPDIR}

CLASSPATH="${CLASSPATH}:${APPDIR}/target/classes/*:${APPDIR}/libraries/jar/*:${APPDIR}/myrobotlab.jar"
export CLASSPATH

echo CLASSPATH=${CLASSPATH}

# TODO move vars to top
# TODO have --id as an optional var

# TODO - option to package jdk for now use bin in path
JAVA=java
# if we decide to package the jvm
if [ -x "${APPDIR}/java/bin/java" ]; then
  JAVA=${APPDIR}/java/bin/java
fi

# verify java exists
if type -p java; then
    echo found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME
    _java="$JAVA_HOME/bin/java"
else
    echo "java is not installed please install java 11 e.g. sudo apt install openjdk-11-jdk "
    exit
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)

if [ "$JAVA_VER" -ge 11 ]; then
    echo "found java version equal or greater to 11"
else
    echo "incompatible version of java, java 11 required"
    exit
fi


# Processing/Arduino handle this in an array - no need for now
JAVA_OPTIONS="-Djava.library.path=libraries/native -Djna.library.path=libraries/native -Dfile.encoding=UTF-8"

if (( $# > 0 )); 
then
  echo "USER SUPPLIED ARGS"
  "${JAVA}" ${JAVA_OPTIONS} -cp ${CLASSPATH} org.myrobotlab.service.Runtime $@
  exit
fi

# IS THIS VALID 2>&1 IF ALREADY GOING TO A LOG FILE - CYBER SAID SOME LOGGING WAS MISSING

if test -f "$REPO_FILE"; then
    echo "$REPO_FILE exists."
else 
    echo "$REPO_FILE does not exist."
    "${JAVA}" ${JAVA_OPTIONS} -cp ${CLASSPATH} org.myrobotlab.service.Runtime --install
fi

"${JAVA}" ${JAVA_OPTIONS} -cp ${CLASSPATH} org.myrobotlab.service.Runtime --log-level info -s log Log security Security webgui WebGui intro Intro python Python

echo $# $@