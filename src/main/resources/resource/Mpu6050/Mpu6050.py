#########################################
# Mpu6050.py
# more info @: http://myrobotlab.org/service/Mpu6050
#########################################
port = '/dev/ttyACM0'
# port = 'COM5'

mpu6050 = runtime.start('mpu6050','Mpu6050')
mpu6050.setDeviceBus('0') # When using the Arduino, we must use bus 0, on the Raspberry Pi use Bus 1
mpu6050.setDeviceAddress('0x68')
mpu6050.setSampleRate(10) # in Hz default is 3Hz

# end test
# raspi controler :
# raspi = runtime.start('RasPi','RasPi')
mega = runtime.start('mega','Arduino')
mega.connect(port)

sleep(3)

# mpu6050.attach(raspi,'1','0x68')
mpu6050.attach(mega)

# for simple orientation
python.subscribe('mpu6050', 'publishOrientation')

# for "all" the data !
python.subscribe('mpu6050', 'publishMpu6050Data')

def onOrientation(data):
    print(data)

def onMpu6050Data(data):
    print(data)

# tell refresh the current mpu data.
mpu6050.start()

