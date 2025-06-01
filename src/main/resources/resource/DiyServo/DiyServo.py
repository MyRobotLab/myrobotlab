# This is a demo of the setup for DiyServo. 
# This setup is valid from version 1.0.2274
# The difference compared to earlier versions is that it now
# starts a MotorDualPwm service that connects to the Arduino
# Before the DiyServo connected directly to the Arduino.
#
# Config
port="COM3"
# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
# Start of script for DiyServo
# Analog input A0 is the same as digital 14 on the Arduino Uno
A0 = 14
# Start the Arduino
arduino = runtime.start("Arduino","Arduino")
arduino.connect(port)
# Start the MotorDualPwm. You can use also use a different type of Motor
motor = runtime.start("diyservo.motor","MotorDualPwm")
# Tell the motor to attach to the Arduino and what pins to use
motor.setPwmPins(10,11)
motor.attach(arduino)
# Start the DiyServo
servo = runtime.start("diyservo","DiyServo")
servo.attach(arduino,A0) # Attach the analog pin 0
# Set the PID values. This example shows what DiyServo has as default.
servo.pid.setPid("diyservo", 0.020, 0.001, 0.0)
servo.moveTo(90)
# At this stage you can use the gui or a script to control the DiyServo
