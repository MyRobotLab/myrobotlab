import math

arduino = Runtime.createAndStart("arduino","Arduino")
sinusoidal = Runtime.createAndStart("sinusoidal","Servo")
arduino.connect("COM3", 57600, 8, 1, 0)
sleep(4)
arduino.attach(sinusoidal.getName() , 3)

rad2grad = 57.295779513
limit =360
inc = 5
global deg
deg = 0
global ang
ang = 0
endang = 70

for i in range(1000):
 
 if (ang > limit):
  ang = 0
 
 number = math.sin(ang/rad2grad)
 deg = int(((number * endang) + 90))
 sinusoidal.moveTo(deg)
 sleep(0.04)
 ang = (ang + inc)
 print deg
 print ang