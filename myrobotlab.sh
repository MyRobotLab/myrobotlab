#!/usr/bin/env bash

REPO_FILE=libraries/repo.json

# fancy way to get real cwd ?
APPDIR="$(dirname -- "$(readlink -f -- "${0}")" )"

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
if command -v ${JAVA}; then
  echo "found java"
else
  echo "ERROR ${JAVA} not found" 
  echo " please install e.g. sudo apt install openjdk-11-jdk"
  exit
fi

# Processing/Arduino handle this in an array - no need for now
JAVA_OPTIONS="-Djava.library.path=libraries/native -Djna.library.path=libraries/native -Dfile.encoding=UTF-8"

if (( $# > 0 )); 
then
  echo "USER SUPPLIED ARGS"
  "${JAVA}" ${JAVA_OPTIONS} -cp ${CLASSPATH} org.myrobotlab.service.Runtime --from-launcher $@
  exit
fi

if test -f "$REPO_FILE"; then
    echo "$REPO_FILE exists."
else 
    echo "$REPO_FILE does not exist."
    "${JAVA}" ${JAVA_OPTIONS} -cp ${CLASSPATH} org.myrobotlab.service.Runtime --from-launcher --install
fi

"${JAVA}" ${JAVA_OPTIONS} -cp ${CLASSPATH} org.myrobotlab.service.Runtime --from-launcher --log-level info -s webgui WebGui intro Intro python Python

echo $# $@