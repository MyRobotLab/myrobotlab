# This Python file uses the following encoding: utf-8

from java.lang import String
import time
import os, sys

'''windows'''
dexarm1 = Dexarm1(port="COM3")
dexarm2 = Dexarm2(port="COM4")
'''mac & linux'''
# device = Dexarm(port="/dev/tty.usbmodem3086337A34381")
sleep(2)
dexarm1.go_home1()
dexarm2.go_home2()
#sleep(2)
#dexarm.set_module_type(0)
#dexarm.writeFile('resource/Dexarm/gcode/letter1.gcode')
#sleep(2)
#dexarm.go_home()
#sleep(1)
#dexarm.go_home()