serial = Runtime.createAndStart("serial","Serial")
serial.connect("COM3",38400,8,1,0)
serial.addListener("publishByte","python","prova")
ax = ""
def prova(data):
    global ax
    code = (data & 0xff)
    if (code !=10 and code !=13):
      ax += chr(code)
    elif (code == 10):
      print ax
      ax = ""