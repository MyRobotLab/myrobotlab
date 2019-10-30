# start the virtual arduino service
varduino = Runtime.start("varduino","VirtualArduino")

# "connect" - creates a virtual COM port with 2 ends
# one called "COM5" the other "COM5.UART"
# the virtual arduino connects to the COM5.UART end,
# and an arduino service can connect to the "COM5" end
varduino.connect("COM5")

# Runtime.start("WebGui","WebGui")
varduino = Runtime.start("varduino","VirtualArduino")

# start the Arduino service
arduino = Runtime.start("arduino","Arduino")
# connect it to the emulator
arduino.connect("COM5")

# start reading from a digital and an analog pin
# "default" emulator values of pin is random
arduino.enablePin("D2")
arduino.enablePin("A3")

# create a servo and attach it
servo = Runtime.start("servo","Servo")
servo.attach(arduino, 7)

servo.moveTo(10)
