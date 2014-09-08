from __future__ import division

arduino = Runtime.start("arduino","Arduino")
i01 = Runtime.createAndStart("i01","InMoov")
i01.startHead("COM5")
arduino.addCustomMsgListener(python)
arduino.connect("COM3")

counter = 0
servo = 90


def onCustomMsg(ax ,ay ,az):
  global counter
  global servo
  counter += 1
  servo = (20 +(((ax - 20000)/(-20000 - 20000))*(160 - 20)))
  if (counter == 25):
      i01.head.neck.moveTo(int(servo))
      counter = 0
