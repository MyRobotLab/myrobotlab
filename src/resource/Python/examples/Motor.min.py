# demonstrates the basic motor api
# an Arduino is used as a motor controller
# this dc motor has a simple h-bridge
# 1 pin controls power/speed with pulse width modulation
# the other controls direction

port = "COM10"

arduino = Runtime.createAndStart("arduino", "Arduino")
arduino.connect(port)

m1 = Runtime.createAndStart("m1","Motor")

# connect motor m1 with pwm power pin 3, direction pin 4
arduino.motorAttach("m1", 3, 4) 

# move both motors forward
# at 50% power
# for 2 seconds
m1.move(0.5)

sleep(2)

# move both motors backward
# at 50% power
# for 2 seconds
m1.move(-0.5)

sleep(2)

# stop and lock m1
m1.stopAndLock()

# after locking
# m1 should not move
m1.move(0.5)

sleep(2)

# unlock m1 and move it
m1.unlock()
m1.move(0.5)

sleep(2)

m1.stop()
