mkdir dev
cd dev
git clone https://github.com/MyRobotLab/InMoov2.git
git clone https://github.com/MyRobotLab/myrobotlab.git
cd myrobotlab
git pull
git status
curl -LOJ http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/artifact/target/myrobotlab.jar
java -jar myrobotlab.jar --install && java -jar myrobotlab.jar -s webgui WebGui python Python i01 InMoov2 intro Intro

