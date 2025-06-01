#
# Example code for Ads1115 4-channel AD converter on the i2c bus.
#
port = "COM3"
#
WebGui = runtime.start("WebGui","WebGui")
ads1115 = runtime.start("Ads1115","Ads1115")

if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)

# This section is to be used if you use the i2c pins of the Arduino
arduino = runtime.start("Arduino","Arduino")
arduino.connect(port)
# Sleep so that the Arduino can be initialized
sleep(4)
ads1115.attach(arduino,"0","0x48") # When using the Arduino, it only has a bus 0.

# This section is to be used if you use the i2c pins of the Raspberry PI
# raspi = runtime.start("Raspi","RasPi")
# ads1115.attach(raspi,"1","0x48")

# This section is common and shows how you can get the raw adc values and the voltages
ads1115.refresh()
print "adc0 raw value", ads1115.adc0
print "adc1 raw value", ads1115.adc1
print "adc2 raw value", ads1115.adc2
print "adc3 raw value", ads1115.adc3
print "adc0 voltage", ads1115.voltage0
print "adc1 voltage", ads1115.voltage1
print "adc2 voltage", ads1115.voltage2
print "adc3 voltage", ads1115.voltage3



