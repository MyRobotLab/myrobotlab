rm myrobotlab.jar
rm myrobotlab-*

mvn -DskipTests -Dbuild.number=180 compile package -o

mv target/myrobotlab.jar .

java -jar myrobotlab.jar -s webgui WebGui i01 InMoov2 intro Intro python Python