#########################################
# Pir.py
# description: PIR - Passive Infrared Sensor
# categories: sensor
# more info @: http://myrobotlab.org/service/Pir
#########################################

from datetime import datetime

# start the service
pir = Runtime.start('pir','Pir')
pir.setPin('23')

# start a micro controller
mega = Runtime.start('mega','Arduino')

# start a speech synthesis service
mouth = Runtime.start('mouth','LocalSpeech')

# connect the micro controller
mega.connect('/dev/ttyACM1')
mega.attach(pir)

# attach the pir sensor to the micro controller
pir.attach('mega')

# subscribe to the pir's publishSense method - callback will be python onSense
python.subscribe('pir', 'publishSense')
# enable the pir
pir.enable()

# callback method - change state of pir this method will be called
def onSense(data):
    print('onSense', data, str(datetime.now()))
    mouth = Runtime.getService('mouth')
    if data:
        mouth.speak('I see you')
    else:
        mouth.speak('where did you go?')
    
