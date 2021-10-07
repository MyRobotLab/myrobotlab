#################################################################
# Example Code                                                  #
#################################################################
 
#################################################################
# First start your I2C Bus Master Device                        #
#################################################################
# If your using the Arduino Nano, comment out this line and
# uncomment the ArDuino Nano lines
raspi = Runtime.start("raspi","RasPi")
#################################################################
# Start the Arduino Nano connected using /dev/ttyUSB0           #
#################################################################
#arduinoNano = Runtime.start("arduinoNano","Arduino")
#arduinoNano.setBoardNano()
#arduinoNano.connect("/dev/ttyUSB0")
 
#################################################################
# Next start the PCF8574 service                                #
#################################################################
pcf = Runtime.start("Pcf","Pcf8574")
# Then attach it to the I2C Bus Master
# When attaching, we specify the Bus Master Device,
# the I2C Bus Number
# and the I2C address
# FIXME - avoid these attaches if possible - the pcf should be configured
# first with bus and address - then just pcf.attach(raspi)
# important for simple service lifecycles ... configuration first, attach next
pcf.attach(raspi,"1","0x27")
 
#pcf.attach(arduinoNano,"0","0x27")
 
#################################################################
# Next start the Hd44780 service                                #
#################################################################
hd44780 = Runtime.start("hd44780","Hd44780")
 
# Once the service has been started, we need to attach it to
# the PCF service
hd44780.attach(pcf)
 
# this will initalise the display.
hd44780.init()
 
# when we want to clear the screen call this
hd44780.clear()
 
# You can turn the backlight on or off.
# True will turn it on, False will turn it off.
hd44780.stBackLight(True)
 
# Filally to send text to the display
hd44780.display("Hello World", 0)
