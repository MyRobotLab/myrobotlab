owm=runtime.start("owm", "OpenWeatherMap")
owm.setKey("YOUR_KEY") #you can use this only once
owm.setUnits("metric") # or imperial
owm.setLang("en") # fr / de ... TODO use locale

# unit is 3 hours steps (1) , from 1 to 40 ( today to 5 days )
# fetch raw data for tomorrow (8) -> because 3*8=24H
# fetch raw data for today (1) -> tomorrow is 1 :

owm.setPeriod(1)
owm.setLocation("Paris,FR")

print "Raw code : ", owm.getWeatherCode()
print "Town ", owm.getLocation()
print "The weather is ", owm.getWeatherDescription()
print owm.getDegrees(), owm.getLocalUnits()
print "Humidity ", owm.getHumidity()
print "Min Degrees ", owm.getMinDegrees()
print "Max Degrees ", owm.getMaxDegrees()
print "Pressure ", owm.getPressure()
print "Wind Speed ", owm.getWindSpeed()
print "Wind Orientation ", owm.getWindOrientation()