# This demo creates and starts an Arduino service
# Connects a serial device on Windows this would COMx 
# Sets the board type
# Then starts polling analog pin 17 which is Analog pin 3
soildMoisture = 0
tempHumidity = 2
leftLight = 4
rightLight = 6
airQuality = 10

comPort = "/dev/ttyACM0"

# autostart irritating for development
webgui.autoStartBrowser(False)
# use local to develop - checked in resources are on the net (remote)
webgui.useLocalResources(True)


# create an Arduino service named arduino
arduino = Runtime.createAndStart("arduino","Arduino")

if not (arduino.isConnected()):
    # set the board type - only important when uploading MRLComm.ino
    arduino.setBoard("mega2560") # atmega168 | mega2560 | etc
    # connect to COM9 - 57600, 8, 1 0 (default)
    arduino.connect(comPort) # TODO - make rentrant

# print the version of MRLComm.ino
print arduino.getVersion()

arduino.softReset() # reset to good known state

arduino.setSampleRate(5000) # 1 - fast 32K - slow :P

# start the analog pin sample to display
# in the oscope
arduino.analogReadPollingStart(soildMoisture)
arduino.analogReadPollingStart(tempHumidity)
arduino.analogReadPollingStart(leftLight)
arduino.analogReadPollingStart(rightLight)
arduino.analogReadPollingStart(airQuality)
arduino.save()
