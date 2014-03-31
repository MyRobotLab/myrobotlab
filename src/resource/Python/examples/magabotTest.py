from org.myrobotlab.service import Arduino
from org.myrobotlab.service import MagaBot
from org.myrobotlab.service import Runtime

from time import sleep

# Create a running instance of the MagaBot Service.

magabot = Runtime.create("magabot","MagaBot")
magabot.startService()
magabot.init("COM8")  # initalize arduino on port specified to 9600 8n1

for x in range(0,20):
  magabot.sendOrder('a')
  sleep(0.5)
  
#magabot.sendOrder('a')
#sleep(0.5)
#magabot.sendOrder('a')
#sleep(0.5)
#magabot.sendOrder('a')
#sleep(0.5)


