#!/bin/sh
# purpose of this script is to publish releases to github
# parameters 

echo "running publish-github.sh";

while getopts b:t: flag
do
    case "${flag}" in
        b) build=${OPTARG};;
        t) token=${OPTARG};;
    esac
done
echo "build: $build";
echo "token: $token";

# from - https://docs.github.com/en/rest/releases/releases#create-a-release
curl -X POST -H "Accept: application/vnd.github+json" -H "Authorization: token $token"  https://api.github.com/repos/MyRobotLab/myrobotlab/releases -d "{\"tag_name\":\"$build\",\"target_commitish\":\"develop\",\"name\":\"$build Nixie\",\"body\":\"Nixie latest myrobotlab.zip can be found here http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/artifact/target/myrobotlab.zip\n Website http://myrobotlab.org \n Discord https://discord.com/channels/887362610360643664/887362610360643667 \",\"draft\":false,\"prerelease\":false,\"generate_release_notes\":true}"
