#########################################
# Pingdar.py
# more info @: http://myrobotlab.org/service/Pingdar
#########################################
# virtual = True

port = "COM3"
trigPin = 7
echoPin = 8
servoPin = 4

gui = runtime.start("gui","SwingGui")

# start controler
arduino = runtime.start("arduino","Arduino")
# start UltrasonicSensor
sr04 = runtime.start("sr04", "UltrasonicSensor")
# start a pingdar
pingdar = runtime.start("pingdar","Pingdar")
# start servo
servo = runtime.start("servo","Servo")

if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
    
# attach all the parts
arduino.connect(port);
sr04.attach(arduino, trigPin, echoPin)
servo.attach(arduino, servoPin);
gui.fullscreen(True);
gui.undockTab("pingdar")

# start pingdar
pingdar.attach(sr04, servo)

# sweep from servo position 10 to 170 step by 1
pingdar.sweep(10, 170)

# continue to sweep 
# for 10 seconds
sleep(10)

# stop
pingdar.stop()
gui.dockTab("pingdar")
gui.fullscreen(False);