# The Adafruit16CServoDriver API is supported through Jython 

servo1 = Runtime.createAndStart("servo1", "Servo")	
pwm =  Runtime.createAndStart("pwm", "Adafruit16CServoDriver")
		
pwm.connect("COM12")

# attach servo1 to pin 0 on the servo driver
pwm.attach(servo1, 0)

servo1.broadcastState()

servo1.moveTo(0)
sleep(1)
servo1.moveTo(90)
sleep(1)
servo1.moveTo(180)
sleep(1)
servo1.moveTo(90)
sleep(1)
servo1.moveTo(0)
sleep(1)
servo1.moveTo(90)
sleep(1)
servo1.moveTo(180)
sleep(1)
servo1.moveTo(90)
sleep(1)
servo1.moveTo(0)
