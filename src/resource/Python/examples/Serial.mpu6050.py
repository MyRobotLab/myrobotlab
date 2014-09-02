from __future__ import division
i01 = Runtime.createAndStart("i01","InMoov")
i01.startHead("COM5")
serial = Runtime.createAndStart("serial","Serial")
serial.connect("COM3",38400,8,1,0)
serial.addListener("publishByte","python","prova")
ax = ""
counter = 0
servoint = 0


def prova( data ):
    global ax
    global counter
    global servoint
    code = (data & 0xff)
    if (code !=10 and code !=13):
      ax += chr(code)
    elif (code == 10):
      servo = (30 +(((int(ax) + 18832)/(18832 + 18832))*(150 - 30)))
      servoint = int(servo)
      print servoint
      counter += 1
      ax= ""
      if (counter == 10):
        i01.head.neck.moveTo(servoint)
        counter = 0
