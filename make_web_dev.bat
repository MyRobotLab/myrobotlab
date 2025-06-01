mkdir dev
cd dev
git clone https://github.com/MyRobotLab/InMoov2.git
git clone https://github.com/MyRobotLab/ProgramAB.git
git clone https://github.com/MyRobotLab/myrobotlab.git
cd myrobotlab
git pull
git status
curl -LOJ https://github.com/MyRobotLab/myrobotlab/releases/latest/download/myrobotlab.jar
java -jar myrobotlab.jar --install && java -jar myrobotlab.jar -s webgui WebGui python Python i01 InMoov2 intro Intro

