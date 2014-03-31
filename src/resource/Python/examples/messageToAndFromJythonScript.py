from jarray import array
from java.lang import String
from java.lang import Class
from org.myrobotlab.service import Runtime
from org.myrobotlab.framework import Message

catcher = Runtime.createAndStart("catcher","TestCatcher")
thrower = Runtime.createAndStart("thrower","TestThrower")

def input():
	# python catches data from thrower - then throws to catcher
    	print 'thrower sent me ', msg_thrower_send.data[0]
    	print 'modifying the ball'
    	msg_thrower_send.data[0]='throw from python->catcher'
    	print 'throwing to catcher now'
    	python.send('catcher', 'catchString', msg_thrower_send.data[0])

# thrower sends data to python
thrower.throwString('python', 'input', 'throw from thrower->python');