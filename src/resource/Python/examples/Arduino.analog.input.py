# getting analog input back from arduino into Python

arduino = Runtime.createAndStart("arduino","Arduino")
arduino.connect("COM12")

readAnalogPin = 1
arduino.analogReadPollingStart(readAnalogPin)
arduino.addListener("publishPin", "python", "publishPin")

def publishPin(pin):
  print pin.pin, pin.value, pin.type, pin.source
  
# arduino.analogReadPollingStop(readAnalogPin)  
