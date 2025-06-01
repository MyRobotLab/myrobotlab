# This is an example of using the Lm75a temperature sensor connected on a WiFi
# network using the esp8266 service. 
# You can also use an Arduino or RasPi service to connect to the Lm75a
port="COM3"

if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
    

lm75a = runtime.start("lm75a","Lm75a")

# esp8266 use
# esp = runtime.start("esp","Esp8266_01")
# esp.setHost("esp8266-02.local")
# lm75a.attach("esp","0","0x48")

# arduino use
arduino = runtime.start("arduino","Arduino")
arduino.connect(port)
lm75a.attach(arduino,"0","0x48")

print lm75a.getTemperature()
print lm75a.getConfig()
print lm75a.getTos()
print lm75a.getThyst()

lm75a.setTos(90)
lm75a.setThyst(88)

print lm75a.getTemperature()
print lm75a.getConfig()
print lm75a.getTos()
print lm75a.getThyst()
