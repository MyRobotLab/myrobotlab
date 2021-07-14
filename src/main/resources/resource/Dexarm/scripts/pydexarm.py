# This Python file uses the following encoding: utf-8

from java.lang import String
#import serial
import re
#import os, sys

#log = Runtime.start("log","Log")
#RuningFolder="scripts"
#RuningFolder=os.getcwd().replace("\\", "/")+"/"+RuningFolder+"/"
#execfile(RuningFolder+'/example.py')

class Dexarm:


    def __init__(self, port):

        #self.ser = serial.Serial(port, 115200, timeout=None)
        #self.is_open = self.ser.isOpen()
        serial = Runtime.start("serial","Serial")
        serial.connect(port, 115200, 8, 1, 0)
        self.ser = serial
        self.is_open = self.ser.isConnected()
        if self.is_open:
            print('pydexarm: %s connected' % self.ser.name)
        else:
            print('failed to connect to serial port')

    def _send_cmd(self, data, wait=True):

        self.ser.write(data.encode())
        if not wait:
            self.ser.reset()
            return
        while True:
            #serial_str = self.ser.readLine().decode('utf-8')
            serial_str = self.ser.readLine()
            if len(serial_str) > 0:
                if serial_str.find("ok") > -1:
                    print("read ok")
                    break
                else:
                    print("read:", serial_str)

    def go_home(self):
        """
        Go to home position and enable the motors. Should be called each time when power on.
        """
        self._send_cmd("M1112\r")

    def set_workorigin(self):
        """
        Set the current position as the new work origin.
        """
        self._send_cmd("G92 X0 Y0 Z0 E0\r")

    def set_acceleration(self, acceleration, travel_acceleration, retract_acceleration=60):
        """
        Set the preferred starting acceleration for moves of different types.

        Args:
            acceleration (int): printing acceleration. Used for moves that employ the current tool.
            travel_acceleration (int): used for moves that include no extrusion.
            retract_acceleration (int): used for extruder retraction moves.
        """
        cmd = "M204"+"P" + str(acceleration) + "T"+str(travel_acceleration) + "T" + str(retract_acceleration) + "\r\n"
        self._send_cmd(cmd)

    def set_module_type(self, module_type):
        """
        Set the type of end effector.

        Args:
            module_type (int):
                0 for Pen holder module
                1 for Laser engraving module
                2 for Pneumatic module
                3 for 3D printing module
        """
        self._send_cmd("M888 P" + str(module_type) + "\r")

    def get_module_type(self):
        """
        Get the type of end effector.

        Returns:
            string that indicates the type of the module
        """
        self.ser.reset()
        self.ser.write('M888\r'.encode())
        while True:
            serial_str = self.ser.readLine().decode('utf-8')
            #serial_str = self.ser.readLine()
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

    def move_to(self, x=None, y=None, z=None, e=None, feedrate=2000, mode="G1", wait=True):
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
        self._send_cmd(cmd, wait=wait)

    def fast_move_to(self, x=None, y=None, z=None, feedrate=2000, wait=True):
        """
        Fast move to a cartesian position, i.e., in mode G0

        Args:
            x, y, z (int): the position, in millimeters by default. Units may be set to inches by G20. Note that the center of y axis is 300mm.
            feedrate (int): sets the feedrate for all subsequent moves
        """
        move_to(self, x=x, y=y, z=z, feedrate=feedrate, mode="G0", wait=wait)

    def get_current_position(self):
        """
        Get the current position
        
        Returns:
            position x,y,z, extrusion e, and dexarm theta a,b,c
        """
        self.ser.reset()
        self.ser.write('M114\r'.encode())
        x, y, z, e, a, b, c = None, None, None, None, None, None, None
        while True:
            serial_str = self.ser.readLine().decode('utf-8')
            #serial_str = self.ser.readLine()
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

    def dealy_ms(self, value):
        """
        Pauses the command queue and waits for a period of time in ms

        Args:
            value (int): time in ms
        """
        self._send_cmd("G4 P" + str(value) + '\r')

    def dealy_s(self, value):
        """
        Pauses the command queue and waits for a period of time in s

        Args:
            value (int): time in s
        """
        self._send_cmd("G4 S" + str(value) + '\r')

    def soft_gripper_pick(self):
        """
        Close the soft gripper
        """
        self._send_cmd("M1001\r")

    def soft_gripper_place(self):
        """
        Wide-open the soft gripper
        """
        self._send_cmd("M1000\r")

    def soft_gripper_nature(self):
        """
        Release the soft gripper to nature state
        """
        self._send_cmd("M1002\r")

    def soft_gripper_stop(self):
        """
        Stop the soft gripper
        """
        self._send_cmd("M1003\r")

    def air_picker_pick(self):
        """
        Pickup an object
        """
        self._send_cmd("M1000\r")

    def air_picker_place(self):
        """
        Release an object
        """
        self._send_cmd("M1001\r")

    def air_picker_nature(self):
        """
        Release to nature state
        """
        self._send_cmd("M1002\r")

    def air_picker_stop(self):
        """
        Stop the picker
        """
        self._send_cmd("M1003\r")

    def laser_on(self, value=0):
        """
        Turn on the laser

        Args:
            value (int): set the power, range form 1 to 255
        """
        self._send_cmd("M3 S" + str(value) + '\r')

    def laser_off(self):
        """
        Turn off the laser
        """
        self._send_cmd("M5\r")

    """Conveyor Belt"""
    def conveyor_belt_forward(self, speed=0):
        """
        Move the belt forward
        """
        self._send_cmd("M2012 F" + str(speed) + 'D0\r')

    def conveyor_belt_backward(self, speed=0):
        """
        Move the belt backward
        """
        self._send_cmd("M2012 F" + str(speed) + 'D1\r')

    def conveyor_belt_stop(self, speed=0):
        """
        Stop the belt
        """
        self._send_cmd("M2013\r")

    """Sliding Rail"""
    def sliding_rail_init(self):
        """
        Sliding rail init.
        """
        self._send_cmd("M2005\r")

    def close(self):
        """
        Release the serial port.
        """
        self.ser.close()
