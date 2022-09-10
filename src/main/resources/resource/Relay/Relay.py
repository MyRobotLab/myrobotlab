#########################################
# Relay.py
# description: Relay used by an arduino
# categories: home automation
# more info @: http://myrobotlab.org/service/Relay
#########################################

# start the service

# port = "/dev/ttyUSB0"
port = "COM15"

# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
    
relay = runtime.start('relay','Relay')

arduino = runtime.start("arduino","Arduino")
arduino.connect(port)

relay.arduino=arduino
relay.pin=8
relay.onValue=0

relay.on()
sleep(2)
relay.off()
