# Dev smoke path for MyRobotLab (Windows).
# Starts Runtime with WebGui + Intro + Python using the Maven classpath.
# Healthy: http://localhost:8888 responds and the process stays up.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

Write-Host "Compiling (skipTests)..."
mvn -q -DskipTests compile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Starting Runtime smoke (WebGui :8888). Ctrl+C to stop."
mvn -q exec:java `
  "-Dexec.mainClass=org.myrobotlab.service.Runtime" `
  "-Dexec.args=--log-level info -s webgui WebGui intro Intro python Python -c dev"
