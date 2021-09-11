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
#dexarm = Dexarm(port="COM3")

'''mac & linux'''
# device = Dexarm(port="/dev/tty.usbmodem3086337A34381")

dexarm2.set_module_type2(2)
#dexarm2.go_home2()

dexarm2.move_to2(50, 300, 0)
dexarm2.move_to2(50, 300, -50)
dexarm2.air_picker_pick2()
dexarm2.move_to2(50, 300, 0)
sleep(1)
dexarm2.move_to2(-50, 300, 0)
dexarm2.move_to2(-50, 300, -50)
#dexarm2.air_picker_place2()
dexarm2.air_picker_nature2()
dexarm2.air_picker_stop2()
dexarm2.move_to2(-50, 300, 0)

dexarm2.go_home2()

## we start dexarm1
dexarm1.set_module_type1(0)
dexarm1.move_to1(50, 300, -30)
#dexarm1.move_to1(G0, X-80.33 Y337.72)
#dexarm1.read_Gcode1()
dexarm1.read_test1()
sleep(1)
dexarm1.go_home1()



'''DexArm sliding rail Demo'''
'''
dexarm2.conveyor_belt_forward2(2000)
time.sleep(20)
dexarm2.conveyor_belt_backward2(2000)
time.sleep(10)
dexarm2.conveyor_belt_stop2()
'''

'''DexArm sliding rail Demo'''
'''
dexarm2.go_home2()
dexarm2.sliding_rail_init2()
dexarm2.move_to2(None,None,None,0)
dexarm2.move_to2(None,None,None,100)
dexarm2.move_to2(None,None,None,50)
dexarm2.move_to2(None,None,None,200)
'''
#dexarm2.close2()