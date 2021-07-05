# This Python file uses the following encoding: utf-8

from java.lang import String
#from pydexarm import Dexarm
import time
import os, sys

'''windows'''
#log = Runtime.start("log","Log")
#RuningFolder="scripts"
#RuningFolder=os.getcwd().replace("\\", "/")+"/"+RuningFolder+"/"
#execfile(RuningFolder+'/pydexarm.py')
dexarm = Dexarm(port="COM3")

'''mac & linux'''
# device = Dexarm(port="/dev/tty.usbmodem3086337A34381")

dexarm.go_home()

dexarm.move_to(50, 300, 0)
dexarm.move_to(50, 300, -50)
dexarm.air_picker_pick()
dexarm.move_to(50, 300, 0)
dexarm.move_to(-50, 300, 0)
dexarm.move_to(-50, 300, -50)
dexarm.air_picker_place()

dexarm.go_home()

dexarm.close()
