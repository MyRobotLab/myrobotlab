#########################################
# AdafruitIna219.py
# description: Adafruit INA219 Voltage and Current sensor Service
# categories: [sensor]
#########################################
# This script shows how to use the AdafruitIna219 service
#
# config
port = "COM3"
# Code to be able to use this script with virtalArduino
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
# 
ina219 = runtime.start("AdafruitIna219","AdafruitIna219")
#
# This section shows is if you use the Arduino i2c pins
# Comment three lines if you don't use the Arduino
arduino = runtime.start("Arduino","Arduino")
arduino.connect(port)
ina219.attach(arduino,"1","0x40")
# 
# This section shows is if you use the GPIO i2c pins on the RaspBerry Pi directly
# Uncomment two lines if you use the RasPi
# raspi = runtime.start("RasPi","RasPi")
# ina219.attach(raspi,"1","0x40")
#
# This sections shows how to get the values from the service
ina219.refresh()
print ina219.busVoltage," mV bus voltage"
print ina219.shuntResistance, "Ohms shunt resistance"
print ina219.shuntVoltage ," mV accross the shunt resistor"
print ina219.current, " mA current"
print ina219.power, " mW power"
