#########################################
# Sweety.py
# description: BeetleJuice's Bot !!!
# categories: robot, servo, control, motor
# more info @: http://myrobotlab.org/service/Sweety
#########################################

import random
import codecs
import socket
from java.lang import String

comPort = "COM3"
board = "atmega2560"

# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    virtualArduino = Runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(comPort)

Runtime.start("sweety", "Sweety")
sweety.arduino.setBoard(board)
sweety.connect(comPort)
sleep(2)
sweety.chatBot.startSession("ProgramAB", "sweety", "sweety")
wdf = Runtime.start("wikiDataFetcher", "WikiDataFetcher") # WikiDataFetcher cherche des données sur les sites wiki
wdf.setLanguage("fr") # on cherche en français
wdf.setWebSite("frwiki") # On fait des recherches sur le site français de wikidata
sweety.mouth.setLanguage("FR") # on parle francais !
sweety.mouth.setVoice("Antoine") # on choisis une voix ( voir la liste des voix sur http://www.acapela-group.com/?lang=fr
sweety.ear.addTextListener(sweety.chatBot) # On creer une liaison de webKitSpeechRecognition vers Program AB
sweety.ear.setLanguage("fr-FR")
sweety.chatBot.addTextListener(sweety.htmlFilter) # On creer une liaison de Program AB vers html filter
sweety.htmlFilter.addListener("publishText", python.name, "talk") # On creer une liaison de htmlfilter vers talk
sweety.chatBot.setPredicate("sweety","prenom","unknow")

sleep(1) # give a second to the arduino for connect
#sweety.startServos()

#sweety.attach()
#sweety.posture("neutral")
#sweety.startUltraSonic()
sweety.mouthState("smile")
sleep(1)
# set delays for led sync (delayTime, delayTimeStop, delayTimeLetter)
sweety.setdelays(50,200,50)


def talk(data):
    if data!="":
        sweety.saying(data)
        #sweety.mouth.speak(data)
        print "chatbot :", data
          
def handOpen():
    sweety.rightArm(-1, -1, -1, -1, 75)
    sweety.leftArm(-1, -1, -1, -1, 80)

def handClose():
    sweety.rightArm(-1, -1, -1, -1, 10)
    sweety.leftArm(-1, -1, -1, -1, 150)
def trackFace():
    sweety.startTrack()
    sweety.eyesTracker.opencv.setCameraIndex(0)
    sweety.eyesTracker.findFace()
def stopTracking():
    sweety.stopTrack()
def askWiki(query):
    query = unicode(query,'utf-8')
    print query
    word = wdf.cutStart(query)
    start = wdf.grabStart(query)
    wikiAnswer = wdf.getDescription(word)
    answer = ( query + " est " + wikiAnswer)
    if wikiAnswer == "Not Found !":
        answer = "Je ne sais pas"
    sweety.chatBot.getResponse("say " + answer)

def getProperty(query, what):
    query = unicode(query,'utf-8')
    what = unicode(what,'utf-8')
    if query[1]== "\'" :
        query2 = query[2:len(query)]
        query = query2
    if what[1]== "\'" :
        what2 = what[2:len(what)]
        what = what2
        print "what = " + what + " - what2 = " + what2
    ID = "error"
    f = codecs.open(u"C:/Users/papa/git/pyrobotlab/home/beetlejuice/propriétés_ID.txt",'r',"utf-8") # set you propertiesID.txt path
    
    for line in f:
            line_textes=line.split(":")
            if line_textes[0]== what:
                ID= line_textes[1]
    f.close()
    print "query = " + query + " - what = " + what + " - ID = " + ID
    wikiAnswer= wdf.getData(query,ID)
    answer = ( what +" de " + query + " est " + wikiAnswer)
    
    if wikiAnswer == "Not Found !":
        answer = "Je ne sais pas"
    sweety.chatBot.getResponse("say " + answer)
    return answer

def getDate(query, ID):
    answer = ( wdf.getTime(query,ID,"day") +" " +wdf.getTime(query,ID,"month") + " " + wdf.getTime(query,ID,"year"))
    print " La date est : " + answer
    sweety.chatBot.getResponse("say Le " + answer)
def getIp():
    ip = str(socket.gethostbyname(socket.gethostname()))
    ip = ip.replace('.',' point ')
    sweety.chatBot.getResponse("say Mon adresse IP est  " + ip)

def math(text):
    text = unicode(text,'utf-8')
    if isinstance(text, basestring):
        text = text.lower()
        print "string"
        text = text.replace('x','*')
        text = text.replace(u'divisé par','/')
        text = eval(text)
    print text
    sweety.chatBot.getResponse("say  " + str(text))