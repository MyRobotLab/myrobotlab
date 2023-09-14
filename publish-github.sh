#!/bin/sh
# purpose of this script is to publish releases to github
# parameters 

echo "running publish-github.sh";

while getopts v:b:t: flag
do
    case "${flag}" in
        v) version=${OPTARG};;
        b) build=${OPTARG};;
        t) token=${OPTARG};;
    esac
done
echo "version: $version";
echo "build: $build";
# echo "token: $token";

# from - https://docs.github.com/en/rest/releases/releases#create-a-release
curl -X POST -H "Accept: application/vnd.github+json" -H "Authorization: token $token"  https://api.github.com/repos/MyRobotLab/myrobotlab/releases -d "{\"tag_name\":\"$version\",\"target_commitish\":\"develop\",\"name\":\"$version Nixie\",\"body\":\"## MyRobotLab Nixie Release\r\n\r\nOpen Source Framework for Robotics and Creative Machine Control\r\n  *You know, for robots!*\r\n\r\n* Project Website http:\/\/myrobotlab.org \r\n* Project Discord https:\/\/discord.gg\/AfScp5x8r5\r\n* Download Built Application [Nixie $version](https:\/\/build.myrobotlab.org:8443\/job\/myrobotlab\/job\/develop\/$build\/artifact\/target\/myrobotlab.zip)\r\n* [JavDocs](https:\/\/build.myrobotlab.org:8443\/job\/myrobotlab\/job\/develop\/$build\/artifact\/target\/site\/apidocs\/org\/myrobotlab\/service\/package-summary.html)\r\n## Base Requirements\r\n\r\nYou will need Java 11 or newer.  If you are only running MyRobotLab you need the JRE (Java Runtime Environment.)  If you are going to be building from source, you'll need the JDK (Java Development Kit) Oracle or OpenJDK will work.\r\n \",\"draft\":false,\"prerelease\":false,\"generate_release_notes\":true}"
