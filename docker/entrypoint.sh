#!/usr/bin/env bash
set -euo pipefail

MRL_HOME="${MRL_HOME:-/opt/mrl}"
cd "${MRL_HOME}"

mkdir -p data/config libraries

# Same idea as Launcher: myrobotlab.jar first (resource/manifest win), then Ivy jars.
# Do not use `java -jar` — that ignores libraries/jar and causes ClassNotFoundException
# for service deps (e.g. org.bytedeco.opencv.* when starting OpenCV).
# Keep the * literal (quoted) so the JVM expands jar wildcards.
CLASSPATH="${MRL_HOME}/myrobotlab.jar:${MRL_HOME}/libraries/jar/*"

# Seed sample configs on first boot (does not overwrite existing dirs).
seed_config() {
  local name="$1"
  if [ ! -f "data/config/${name}/runtime.yml" ] && [ -d "docker-config/${name}" ]; then
    echo "Seeding config set '${name}' into data/config/${name}"
    mkdir -p "data/config/${name}"
    cp -a "docker-config/${name}/." "data/config/${name}/"
  fi
}

seed_config default
seed_config inmoov

# Fallback only: image build normally runs a full --install. If an empty volume
# was mounted over libraries/, restore by installing everything once.
if [ ! -f "libraries/repo.json" ]; then
  if [ "${MRL_INSTALL:-all}" != "none" ]; then
    echo "libraries/repo.json missing — installing all service dependencies"
    # shellcheck disable=SC2086
    java ${JAVA_OPTS:-} -cp "${CLASSPATH}" org.myrobotlab.service.Runtime --install
  fi
fi

# If no Runtime args were supplied, start the standard service set.
if [ "$#" -eq 0 ]; then
  set -- --log-level info -s log Log security Security webgui WebGui intro Intro python Python
fi

echo "Starting MyRobotLab in ${MRL_HOME}"
echo "  java ${JAVA_OPTS:-} -cp ${CLASSPATH} org.myrobotlab.service.Runtime $*"
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -cp "${CLASSPATH}" org.myrobotlab.service.Runtime "$@"
