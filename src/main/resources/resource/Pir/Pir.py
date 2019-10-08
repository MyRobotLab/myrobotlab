#########################################
# Pir.py
# description: PIR - Passive Infrared Sensor
# categories: sensor
# more info @: http://myrobotlab.org/service/Pir
#########################################

# start the service
pir = Runtime.start('pir','Pir')

# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    virtualArduino = Runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect("COM4")

# start the controler
arduino = Runtime.start("arduino","Arduino")

# connect it
arduino.connect("COM4")
arduino.setBoardMega() # used for pin reference
pir.attach(arduino,2 ) # arduino is controler like i2c arduino ... / 2 is pin number

# pir start
pir.isVerbose=True
pir.enable(1) # 1 is how many time / second we poll the pir

# event listener
pir.addListener("publishSense",python.name,"publishSense")

def publishSense(event):
  if event:print "Human detected !!!"