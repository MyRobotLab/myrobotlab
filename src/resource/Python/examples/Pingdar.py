# adjust for your system

port = "COM12"
trigPin = 7
echoPin = 8
servoPin = 4

# start a pingdar
pingdar = Runtime.start("pingdar","Pingdar")

# attach all the parts
pingdar.attach(port, trigPin, echoPin, servoPin)

# sweep from servo position 10 to 170 step by 1
pingdar.sweep(10, 170, 1)

# continue to sweep 
# for 10 seconds
sleep(10)

# stop
pingdar.stop()