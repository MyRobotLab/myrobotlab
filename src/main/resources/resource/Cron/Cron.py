# The following script will send a quote to a Log and Python every minute
from datetime import datetime

cron  =  Runtime.createAndStart('cron', 'Cron')
log   =  Runtime.createAndStart('log', 'Log')
speech = Runtime.createAndStart("speech","MarySpeech")

# add a task which sends text to the log service Log.log(string) every minute
cron.addTask('* * * * *','log','log', 'hello sir, time for your coffee')
# add a task to send text to a python function every minute
cron.addTask('* * * * *','python','doThisEveryMinute', 'hello sir, time for your coffee')

dateObj = datetime
print dateObj.now()

def doThisEveryMinute(text):
  print dateObj.now()
  print dateObj.time(dateObj.now()),text
  speech.speak(text)

listOfTasks = cron.getCronTasks()
for i in listOfTasks:
  print(i.name, i.cronPattern, i.method)