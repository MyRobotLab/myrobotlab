#!/usr/bin/env bash
# Dev smoke path for MyRobotLab (Unix).
# Starts Runtime with WebGui + Intro + Python using the Maven classpath.
# Healthy: http://localhost:8888 responds and the process stays up.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "Compiling (skipTests)..."
mvn -q -DskipTests compile

echo "Starting Runtime smoke (WebGui :8888). Ctrl+C to stop."
mvn -q exec:java \
  -Dexec.mainClass=org.myrobotlab.service.Runtime \
  -Dexec.args="--log-level info -s webgui WebGui intro Intro python Python -c dev"
