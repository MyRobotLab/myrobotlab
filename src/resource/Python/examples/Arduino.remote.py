# How to remotely compile an load MRLComm.ino 

from org.myrobotlab.fileLib import FileIO
python.send("arduino", "upload", FileIO.getResourceFile("Arduino/MRLComm/MRLComm.ino"))