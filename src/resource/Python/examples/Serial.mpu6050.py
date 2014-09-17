from __future__ import division

arduino = Runtime.start("arduino","Arduino")
i01 = Runtime.createAndStart("i01","InMoov")
i01.startHead("COM5")
i01.startLeftArm("COM5")
arduino.addCustomMsgListener(python)
arduino.connect("COM3")
i01.leftArm.bicep.setMinMax(5,80)

servo = 90
bicep = 5


def onCustomMsg(ax ,ay2 ,az):
  global head
  global bicep
  head = (20 +(((ax - 16000)/(-16000 - 16000))*(160 - 20)))
  bicep = (85 +(((ay2 - 20)/(-250 - 20))*(5 - 85)))
  i01.head.neck.moveTo(int(head))
  i01.leftArm.bicep.moveTo(int(bicep))
