# This Python file uses the following encoding: utf-8

from java.lang import String
#from pydexarm import Dexarm
import time
import os, sys

# We move dexarm1 away
dexarm1.move_to1(140, 180, 50)

# We start dexarm2 to set the paper
dexarm2.set_module_type2(2)
dexarm2.set_module_type2(6)
dexarm2.move_to2(170, 330, 10)
dexarm2.move_to2(170, 330, 0)
dexarm2.air_picker_pick2()
dexarm2.rotate_wrist2()
dexarm2.move_to2(170, 330, 10)
dexarm2.move_to2(0, 330, 10)
dexarm2.move_to2(0, 330, 0)
#dexarm2.air_picker_place2()
dexarm2.air_picker_nature2()
sleep(1)
dexarm2.air_picker_stop2()
dexarm2.rotate_wrist2()
dexarm2.move_to2(0, 330, 10)
dexarm2.move_to2(140, 180, 50)



# We start dexarm1 to write on paper
dexarm1.set_module_type1(0)
#dexarm1.move_to1(50, 300, -30)
#dexarm1.send_gcode1()
dexarm1.read_test1()

# We move dexarm1 away
dexarm1.move_to1(140, 180, 50)

# We start dexarm2 to remove the written paper
dexarm2.set_module_type2(2)
dexarm2.set_module_type2(6)
dexarm2.move_to2(0, 330, 10)
dexarm2.move_to2(0, 330, 0)
dexarm2.air_picker_pick2()
dexarm2.rotate_wrist2()
dexarm2.move_to2(0, 330, 10)
dexarm2.move_to2(-170, 330, 10)
dexarm2.move_to2(-170, 330, 0)
#dexarm2.air_picker_place2()
dexarm2.air_picker_nature2()
sleep(1)
dexarm2.air_picker_stop2()
dexarm2.rotate_wrist2()
dexarm2.move_to2(-170, 330, 10)



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
