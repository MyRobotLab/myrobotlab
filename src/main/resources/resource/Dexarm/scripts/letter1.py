# This Python file uses the following encoding: utf-8

from java.lang import String
import time
import os, sys

'''windows'''
dexarm = Dexarm(port="COM3")

'''mac & linux'''
# device = Dexarm(port="/dev/tty.usbmodem3086337A34381")

dexarm.go_home()
serial.writeFile('dexarm/gcode/letter1.gcode')
