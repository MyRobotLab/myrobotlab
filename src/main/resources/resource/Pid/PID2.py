arduino = runtime.start("arduino","Arduino")
arduino.connect("COM3")
# arduino.setSampleRate(30000)


readAnalogPin = 0
arduino.arduino.enablePin(readAnalogPin)
arduino.addListener("publishPin", "python", "input")


pid = runtime.start("pid","Pid")
pid.setMode(1)
#set the range of the "correction"
pid.setOutputRange(-5, 5)
#set Kp, kd, ki kp = gain, how strong it react kd = how fast it react ki= take care of the sum of errors (differences between target and actual value) in the time
pid.setPID(10.0, 0, 1.0)
pid.setControllerDirection(0)

#set a starting analog value, which will pilot the MOSFET on the Gate
heaterValue = 512


def input():
 thermistorPin = msg_arduino_publishPin.data[0]
 print 'thermistor value is', thermistorPin.value
 global heaterValue
 global futureHeaterValue
 #target of temperature or target value
 pid.setSetpoint(150)
 #input value
 pid.setInput(thermistorPin.value)
 pid.compute()
 correction = pid.getOutput()
 futureHeaterValue = (heaterValue + correction)
 if (futureHeaterValue < 1024) and (futureHeaterValue >0):
  heaterValue = futureHeaterValue
  arduino.analogWrite(4,futureHeaterValue)
  print heaterValue
 else :
  arduino.analogWrite(4,heaterValue)
  print heaterValue
  
