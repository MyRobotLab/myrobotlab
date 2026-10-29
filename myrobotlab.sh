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
elif ! command -v java >/dev/null 2>&1; then
  if [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    echo found java executable in JAVA_HOME
    JAVA="$JAVA_HOME/bin/java"
  else
    echo "Java was not found. MyRobotLab requires Java 17.0.20 or newer."
    echo "For example: sudo apt install openjdk-17-jdk"
    exit 1
  fi
fi

# Java 17.0.19 and older ship a Visual C++ runtime that cannot load the OpenCV
# natives on Windows (UnsatisfiedLinkError on jniopencv_core.dll).
JAVA_SPEC=$("$JAVA" -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///')
JAVA_MAJOR=$(printf '%s' "$JAVA_SPEC" | cut -d'.' -f1 | cut -d'-' -f1 | cut -d'+' -f1)
JAVA_UPDATE=$(printf '%s' "$JAVA_SPEC" | cut -d'.' -f3 | cut -d'_' -f1 | cut -d'+' -f1 | cut -d'-' -f1)
if [ -z "$JAVA_UPDATE" ]; then
  JAVA_UPDATE=0
fi

JAVA_TOO_OLD=0
if [ "$JAVA_MAJOR" -lt 17 ]; then
  JAVA_TOO_OLD=1
elif [ "$JAVA_MAJOR" -eq 17 ] && [ "$((10#$JAVA_UPDATE))" -lt 20 ]; then
  JAVA_TOO_OLD=1
fi

if [ "$JAVA_TOO_OLD" -eq 1 ]; then
  echo "Your Java version (${JAVA_SPEC}) is too old."
  echo "MyRobotLab requires Java 17.0.20 or newer."
  echo "Older Java 17 builds include a Visual C++ runtime that is too old to load the OpenCV native libraries on Windows."
  echo "Starting OpenCV then fails with: UnsatisfiedLinkError: jniopencv_core.dll: Can't find dependent libraries."
  echo "Install Java 17.0.20 or later, or a newer release such as Java 21 or 25, and run MyRobotLab with that java."
  exit 1
fi
echo "found java ${JAVA_SPEC}"


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