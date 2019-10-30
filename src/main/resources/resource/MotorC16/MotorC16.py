# The motor service can also be connected to an Adafruit 16-channel servo driver
# The limitation is that you have to choose between using it to drive servos or motors, not both at the same time.
# The reason is that only one pwm frequency can be set for all pins
# This is how you use the motor service when it's connected to an Adafruit 16-channel servo driver thru the Arduino
arduino = Runtime.createAndStart("Arduino", "Arduino");
arduino.connect("COM3")
ada = Runtime.createAndStart("Ada","Adafruit16CServoDriver")
ada.setController(arduino,"1","0x40")
motor01 = Runtime.createAndStart("motor01", "Motor");
motor01.setPwmPins(0,1);
motor01.attach(ada);
motor01.move(0.3);

# This is how you use the motor service when it's connected to an Adafruit 16-channel servo driver connected to the i2c GPIO pins on the Raspberry PI
raspi = Runtime.createAndStart("RasPi", "RasPi");
ada = Runtime.createAndStart("Ada","Adafruit16CServoDriver")
ada.setController(raspi,"1","0x40")
motor01 = Runtime.createAndStart("motor01", "Motor");
motor01.setPwmPins(0,1);
motor01.attach(ada);
motor01.move(0.3);
