# This demo creates and starts an Arduino service
# Connects a serial device on Windows this would COMx 
# Sets the board type
# Then starts polling analog pin 17 which is Analog pin 3

# create an Arduino service named arduino
runtime.createAndStart("arduino","Arduino")

# set the board type
arduino.setBoard("atmega328") # atmega168 | mega2560 | etc

# set serial device
arduino.connect("/dev/ttyUSB1",57600,8,1,0)
sleep(1) # give it a second for the serial device to get ready

# update the gui with configuration changes
arduino.publishState()

# start the analog pin sample to display
# in the oscope
arduino.analogReadPollingStart(17)

# sample the data in the oscope for 10 seconds
sleep(10) 

# turn off the sampling
arduino.analogReadPollingStop(17)
