from org.myrobotlab.service import Runtime
from org.myrobotlab.service import WebServer
from org.myrobotlab.net import BareBonesBrowserLaunch

#It creates and starts webserver service
webserver = Runtime.createAndStart("webserver","WebServer")
#Launch your web browser to the URL of localhost, by default port is 19191
#helloworl.htm file is in your MRL main folder
#you can add files there and change the URL below as you desire
BareBonesBrowserLaunch.openURL("http://localhost:19191/helloworld.htm")

