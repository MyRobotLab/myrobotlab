# The following script will send a quote to a Log and Speech service every minute.

speech = Runtime.createAndStart("speech","Speech")
cron   =  Runtime.createAndStart("cron", "Cron")
log   =  Runtime.createAndStart("log", "Log")
cron.addScheduledEvent("* * * * *","speech","speak", "hello sir, time for your coffee")
cron.addScheduledEvent("* * * * *","log","log", "hello sir, time for your coffee")