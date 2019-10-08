import org.myrobotlab.service.Serial as Serial

port = "COM12"

ssc = Runtime.start("ssc", "Ssc32UsbServoController")

# connect to port with "default" 9600
# ssc.connect(port)
# you can change baud rate with instructions here
# http://myrobotlab.org/service/ssc32usbservocontroller
# i changed my baud rate to 115200
ssc.connect(port, Serial.BAUD_115200)

#grab serial if you want
serial = ssc.getSerial()

# make a servo
servo = Runtime.start("servo", "Servo")

# set its pin
servo.setPin(27) 

# attach a servo !
ssc.attach(servo)

# servo.setVelocity(10)
# move it around
servo.moveTo(0)
sleep(1)
servo.moveTo(90)
sleep(1)
servo.moveTo(180)
sleep(1)
servo.moveTo(10)
sleep(1)
# disable it
servo.disable()
# try to move it - it wont move
servo.moveTo(180)
sleep(1)
# re-enable it - it should move now
servo.enable()
servo.moveTo(10)
sleep(1)
servo.moveTo(160)

# detach it
ssc.detach(servo)