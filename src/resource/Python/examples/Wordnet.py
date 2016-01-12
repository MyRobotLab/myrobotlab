###############################################################################
# Example of using program ab with solr for question answering and reasoning
# on top of an index with Wordnet index.
#
# Topology: Webgui -> WebKitSpeechRecognition -> ProgramAB -> 
#                     Python(->Solr) -> ProgramAB -> AcapelaSpeech.
#
###############################################################################
from org.myrobotlab.service import Solr
from org.myrobotlab.service import AcapelaSpeech

######################################################################
# start swing and webgui
######################################################################
webgui = Runtime.createAndStart("webgui", "WebGui")
gui = Runtime.createAndStart("gui", "GUIService")

######################################################################
# start solr connection
######################################################################
solr = Runtime.createAndStart("solr", "Solr")
solr.setSolrUrl("http://phobos:8983/solr/graph")
######################################################################
#start programab
######################################################################
programab = Runtime.createAndStart("programab", "ProgramAB")
programab.startSession("c:/dev/workspace.kmw/mrl2/myrobotlab/test/ProgramAB/", "username", "lloyd")
######################################################################
# Create the webkit speech recognition gui
######################################################################
wksr = Runtime.createAndStart("webkitspeechrecognition", "WebkitSpeechRecognition")
######################################################################
# create the html filter to filter the output of program ab
######################################################################
htmlfilter = Runtime.createAndStart("htmlfilter", "HtmlFilter")
######################################################################
# the mouth
######################################################################
mouth = Runtime.createAndStart("mouth", "AcapelaSpeech")

# add a link between the webkit speech to publish to ProgramAB
wksr.addTextListener(programab)
# Add route from Program AB to html filter
programab.addTextListener(htmlfilter)
# Add route from html filter to mouth
htmlfilter.addTextListener(mouth)

# OK here we are everything is wired and started.
def isXY(x,y):
    global solr
    print "X WAS :" + str(x)
    print "Y WAS :" + str(y)
    q = "+{!graph from=\"synset_id\" to=\"hypernym_id\"}sense_lemma:\""+str(x.lower().replace(" ","_"))+"\" +sense_lemma:\""+str(y.lower().replace(" ","_"))+"\""
    print "QUERY STRING " + q
    response = solr.search(q)
    numFound = response.getResults().size()
    gloss = response.getResults().get(0).getFieldValue("sense_gloss")
    print "Found " + str(numFound)
    if numFound > 0:
        mouth.speak(str(gloss) + "Yes, a " + str(x) + " is a " + str(y))
    else:
        mouth.speak("No")
    
