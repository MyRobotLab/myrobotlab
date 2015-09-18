# simple script to show how to send a message to and recieve a message from
# using a robot with the XMPP service 

# create an xmpp service
xmpp = Runtime.createAndStart("xmpp","XMPP")

# adds the python service as a listener for messages
xmpp.addListener("python","publishMessage")

# there is a big list of different xmpp/jabber servers out there
# but we will connect to the big one - since that is where our robots account is
xmpp.connect("talk.google.com", 5222, "robot01@myrobotlab.org", "xxxxxxx")

# gets list of all the robots friends
print xmpp.getRoster()

# set your online status
xmpp.setStatus(True, "online all the time")

# add auditors you want this robot to chat with
# auditors can issue commands and will be notified of 
# commands being sent by others and what those commands return
xmpp.addAuditor("Joe Smith")
xmpp.addAuditor("Jane Smith")

# send a message
xmpp.sendMessage("hello this is robot01 - the current heatbed temperature is 40 degrees celcius", "Joe Smith")

def publishMessage():
	msg = msg_xmpp_publishMessage.data[0]
	print msg.getFrom(), " says " , msg.getBody()