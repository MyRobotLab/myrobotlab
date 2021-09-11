# This Python file uses the following encoding: utf-8

from java.lang import String
#import serial
import re
import time
import os, sys

#log = Runtime.start("log","Log")
#RuningFolder="scripts"
#RuningFolder=os.getcwd().replace("\\", "/")+"/"+RuningFolder+"/"
#execfile(RuningFolder+'/example.py')
#dexarm1 = Runtime.start("dexarm1","Serial")
#dexarm1.connect("COM3", 115200, 8, 1, 0)

#dexarm1 = Dexarm1(port="COM3")
class Dexarm1:


    def __init__(self, port):

        #self.ser1 = serial.Serial(port, 115200, timeout=None)
        #self.is_open = self.ser1.isOpen()
        dexarm1 = Runtime.start("dexarm1","Serial")
        dexarm1.connect(port, 115200, 8, 1, 0)
        self.ser1 = dexarm1
        self.is_open = self.ser1.isConnected()
        if self.is_open:
            print('pydexarm: %s connected' % self.ser1.name)
        else:
            print('failed to connect to serial port')

    def _send_cmd1(self, data, wait=True):

        self.ser1.write(data.encode())
        if not wait:
            self.ser1.reset()
            return
        while True:
            serial_str = self.ser1.readString().decode("utf-8")
            if len(serial_str) > 0:
                if serial_str.find("ok") > -1:
                    print("read ok")
                    break
                else:
                    print("read:", serial_str)

    path = "resource/Dexarm/gcode/hourse.gcode"
    def send_gcode1(self, path, wait=True):
        #This is a personal test.
        self.ser1.write(path.encode())
        if not wait:
            self.ser1.reset()
            return
        while True:
            serial_str = self.ser1.readString().decode("utf-8")
            if len(serial_str) > 0:
                if serial_str.find("ok") > -1:
                    print("read ok")
                    break
                else:
                    print("read:", serial_str)                

    def go_home1(self):
        """
        Go to home position and enable the motors. Should be called each time when power on.
        """
        self._send_cmd1("M1112\r")

    #def move_down1(self):
        #This is a personal test.
        #self._send_cmd1("X0 Y0 Z-10 E0\r")

    #def set_relative1(self):
        #This is a personal test.
        #self._send_cmd1("G91\r")

    #def set_absolute1(self):
        #This is a personal test.
        #self._send_cmd1("G90\r")

    def read_test1(self):
        #This is a personal test.
        self._send_cmd1("G0 X-80.33 Y337.72\r")
        #self._send_cmd1("G1 Z-40\r")
        self._send_cmd1("G1 X-80.02 Y338.19\r")
        self._send_cmd1("G1 X-79.70 Y338.35\r")
        self._send_cmd1("G1 X-79.55 Y338.19\r")
        self._send_cmd1("G1 X-79.55 Y338.03\r")
        self._send_cmd1("G1 X-79.70 Y337.40\r")
        self._send_cmd1("G1 X-79.86 Y336.93\r")
        self._send_cmd1("G0 Z5\r")   

    def disconnect1(self):
        #This is a personal test.
        self.ser1.disconnect()    

    def read_Gcode1(self):
        #This is a personal test.
        #path = "resource/Dexarm/gcode/hourse.gcode"
        #self.ser1.write(path.encode())
        self.ser1.writeFile('resource/Dexarm/gcode/hourse.gcode')
        #self._send_cmd1("path\r")    

    def set_workorigin1(self):
        """
        Set the current position as the new work origin.
        """
        self._send_cmd1("G92 X0 Y0 Z0 E0\r")

    def set_acceleration1(self, acceleration, travel_acceleration, retract_acceleration=60):
        """
        Set the preferred starting acceleration for moves of different types.

        Args:
            acceleration (int): printing acceleration. Used for moves that employ the current tool.
            travel_acceleration (int): used for moves that include no extrusion.
            retract_acceleration (int): used for extruder retraction moves.
        """
        cmd = "M204"+"P" + str(acceleration) + "T"+str(travel_acceleration) + "T" + str(retract_acceleration) + "\r\n"
        self._send_cmd1(cmd)

    def set_module_type1(self, module_type):
        """
        Set the type of end effector.

        Args:
            module_type (int):
                0 for Pen holder module
                1 for Laser engraving module
                2 for Pneumatic module
                3 for 3D printing module
        """
        self._send_cmd1("M888 P" + str(module_type) + "\r")

    def get_module_type1(self):
        """
        Get the type of end effector.

        Returns:
            string that indicates the type of the module
        """
        self.ser1.reset()
        self.ser1.write('M888\r'.encode())
        while True:
            serial_str = self.ser1.readString().decode("utf-8")
            #serial_str = self.ser1.readLine()
            if len(serial_str) > 0:
                if serial_str.find("PEN") > -1:
                    module_type = 'PEN'
                if serial_str.find("LASER") > -1:
                    module_type = 'LASER'
                if serial_str.find("PUMP") > -1:
                    module_type = 'PUMP'
                if serial_str.find("3D") > -1:
                    module_type = '3D'
            if len(serial_str) > 0:
                if serial_str.find("ok") > -1:
                    return module_type

    def move_to1(self, x=None, y=None, z=None, e=None, feedrate=2000, mode="G1", wait=True):
        """
        Move to a cartesian position. This will add a linear move to the queue to be performed after all previous moves are completed.

        Args:
            mode (string, G0 or G1): G1 by default. use G0 for fast mode
            x, y, z (int): The position, in millimeters by default. Units may be set to inches by G20. Note that the center of y axis is 300mm.
            feedrate (int): set the feedrate for all subsequent moves
        """
        cmd = mode + "F" + str(feedrate)
        if x is not None:
            cmd = cmd + "X"+str(round(x))
        if y is not None:
            cmd = cmd + "Y" + str(round(y))
        if z is not None:
            cmd = cmd + "Z" + str(round(z))
        if e is not None:
            cmd = cmd + "E" + str(round(e))
        cmd = cmd + "\r\n"
        self._send_cmd1(cmd, wait=wait)

    def fast_move_to1(self, x=None, y=None, z=None, feedrate=2000, wait=True):
        """
        Fast move to a cartesian position, i.e., in mode G0

        Args:
            x, y, z (int): the position, in millimeters by default. Units may be set to inches by G20. Note that the center of y axis is 300mm.
            feedrate (int): sets the feedrate for all subsequent moves
        """
        move_to1(self, x=x, y=y, z=z, feedrate=feedrate, mode="G0", wait=wait)

    def get_current_position1(self):
        """
        Get the current position
        
        Returns:
            position x,y,z, extrusion e, and dexarm theta a,b,c
        """
        self.ser1.reset()
        self.ser1.write('M114\r'.encode())
        x, y, z, e, a, b, c = None, None, None, None, None, None, None
        while True:
            serial_str = self.ser1.readString().decode("utf-8")
            #serial_str = self.ser1.readLine()
            if len(serial_str) > 0:
                if serial_str.find("X:") > -1:
                    temp = re.findall(r"[-+]?\d*\.\d+|\d+", serial_str)
                    x = float(temp[0])
                    y = float(temp[1])
                    z = float(temp[2])
                    e = float(temp[3])
            if len(serial_str) > 0:
                if serial_str.find("DEXARM Theta") > -1:
                    temp = re.findall(r"[-+]?\d*\.\d+|\d+", serial_str)
                    a = float(temp[0])
                    b = float(temp[1])
                    c = float(temp[2])
            if len(serial_str) > 0:
                if serial_str.find("ok") > -1:
                    return x, y, z, e, a, b, c

    def dealy_ms1(self, value):
        """
        Pauses the command queue and waits for a period of time in ms

        Args:
            value (int): time in ms
        """
        self._send_cmd1("G4 P" + str(value) + '\r')

    def dealy_s1(self, value):
        """
        Pauses the command queue and waits for a period of time in s

        Args:
            value (int): time in s
        """
        self._send_cmd1("G4 S" + str(value) + '\r')

    def soft_gripper_pick1(self):
        """
        Close the soft gripper
        """
        self._send_cmd1("M1001\r")

    def soft_gripper_place1(self):
        """
        Wide-open the soft gripper
        """
        self._send_cmd1("M1000\r")

    def soft_gripper_nature1(self):
        """
        Release the soft gripper to nature state
        """
        self._send_cmd1("M1002\r")

    def soft_gripper_stop1(self):
        """
        Stop the soft gripper
        """
        self._send_cmd1("M1003\r")

    def air_picker_pick1(self):
        """
        Pickup an object
        """
        self._send_cmd1("M1000\r")

    def air_picker_place1(self):
        """
        Release an object
        """
        self._send_cmd1("M1001\r")

    def air_picker_nature1(self):
        """
        Release to nature state
        """
        self._send_cmd1("M1002\r")

    def air_picker_stop1(self):
        """
        Stop the picker
        """
        self._send_cmd1("M1003\r")

    def laser_on1(self, value=0):
        """
        Turn on the laser

        Args:
            value (int): set the power, range form 1 to 255
        """
        self._send_cmd1("M3 S" + str(value) + '\r')

    def laser_off1(self):
        """
        Turn off the laser
        """
        self._send_cmd1("M5\r")

    """Conveyor Belt"""
    def conveyor_belt_forward1(self, speed=0):
        """
        Move the belt forward
        """
        self._send_cmd1("M2012 F" + str(speed) + 'D0\r')

    def conveyor_belt_backward1(self, speed=0):
        """
        Move the belt backward
        """
        self._send_cmd1("M2012 F" + str(speed) + 'D1\r')

    def conveyor_belt_stop1(self, speed=0):
        """
        Stop the belt
        """
        self._send_cmd1("M2013\r")

    """Sliding Rail"""
    def sliding_rail_init1(self):
        """
        Sliding rail init.
        """
        self._send_cmd1("M2005\r")

    def close1(self):
        """
        Release the serial port.
        """
        self.ser1.close()
