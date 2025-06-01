# config 
port = "COM11"
# Code to be able to use this script with virtalArduino
#virtual=1
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
arduino = runtime.start("arduino","Arduino")
arduino.connect(port)

bno = runtime.start("bno","Bno055")

# From version 1.0.2316 use attach instead of setController
# bno.setController(arduino)
bno.attach(arduino,"1","0x28")
#3v=bno.attach(arduino,"1","0x29")


# interrupt pin is attach to pin 8
bno.attachInterruptPin(arduino, 8)

#this method is called each time the sensor report an interrupt
def onInterrupt(data):
  event = bno.getOrientationEuler()
  print event.yaw
  print event.roll
  print event.pitch
  #be sure to reset the interrupt so we can get new one
  bno.resetInterrupt()


bno.addListener("publishInterrupt","python","onInterrupt")

if bno.begin():
  #manually set the calibration offset. you can get the data from getCalibrationOffset(Device) after a successful calibration
  bno.setCalibrationOffset(bno.Device.ACCELEROMETER, 0.40, 0.55, 0.20, bno.Unit.ACC_M_S2)
  bno.setCalibrationOffset(bno.Device.GYROSCOPE, -0.1875, -0.0625, 0.125, bno.Unit.ANGULAR_RATE_DPS)
  bno.enablePin()
  #enable Interrupt(type of interrupt, xAxis, yAxis, zAxis, threshold, duration) see BNO055 documentation for appropriate value
  bno.enableInterrupt(bno.InterruptType.ACC_AM, True, True, True, 30, 1)
  #bno.enableInterrupt(bno.InterruptType.GYR_AM, True, True, True, 0.5, 4)
