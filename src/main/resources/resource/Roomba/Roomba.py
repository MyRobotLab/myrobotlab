from org.myrobotlab.service import Runtime
from org.myrobotlab.service import Roomba
from time import sleep
 
roomba = Runtime.create("roomba","Roomba")
roomba.connect( "COM9" )
roomba.startService()
roomba.startup()
roomba.control()
roomba.playNote( 72, 10 )
roomba.sleep( 200 )
roomba.goForward()
roomba.sleep( 1000 )
roomba.goBackward()
roomba.sleep( 1000 )
roomba.spinRight()
roomba.sleep( 1000 )
