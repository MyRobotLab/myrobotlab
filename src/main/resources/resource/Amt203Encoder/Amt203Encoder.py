#########################################
# ArduAmt203Encoderino.py
# description: service to connect and control to an Amt203Encoder
# categories: encoder, servo, control, motor
# more info @: http://myrobotlab.org/service/Amt203Encoder
#########################################
runtime.setVirtual(True)


port = "COM7"
pin = 3

# start the encoder
encoder = runtime.start("encoder", "Amt203Encoder")
encoder.setPin(pin)

# start an arduino its attached to
mega = runtime.start("mega", "Arduino")
mega.connect(port)
mega.setDebug(True)
mega.attachEncoderControl(encoder)
sleep(1)

# set the zeropoint - all values published will 
# be relative to its current position
encoder.setZeroPoint()
