#################################################################
# Example Code                                                  #
#################################################################
 
#################################################################
# First start your I2C Bus Master Device                        #
#################################################################
# If your using the Arduino Nano, comment out this line and
# uncomment the ArDuino Nano lines
raspi = runtime.start("raspi","RasPi")
#################################################################
# Start the Arduino Nano connected using /dev/ttyUSB0           #
#################################################################
#arduinoNano = runtime.start("arduinoNano","Arduino")
#arduinoNano.setBoardNano()
#arduinoNano.connect("/dev/ttyUSB0")
 
#################################################################
# Next start the PCF8574 service                                #
#################################################################
pcf = runtime.start("pcf","Pcf8574")
# Then attach it to the I2C Bus Master
# When attaching, we specify the Bus Master Device,
# the I2C Bus Number
# and the I2C address
pcf.setBus("1")
pcf.setAddress("0x27")
pcf.attach(raspi)
 
#pcf.attach(arduinoNano,"0","0x27")
 
#################################################################
# Next start the Hd44780 service                                #
#################################################################
lcd = runtime.start("lcd","Hd44780")
 
# Once the service has been started, we need to attach it to
# the PCF service
lcd.attach(pcf)
 
# this will initalise the display.
# not needed now unless you want to manually reset
# lcd.reset() 
 
# when we want to clear the screen call this
lcd.clear()
 
# You can turn the backlight on or off.
# True will turn it on, False will turn it off.
lcd.setBackLight(True)
 
# Filally to send text to the display
lcd.display("Hello World", 0)
