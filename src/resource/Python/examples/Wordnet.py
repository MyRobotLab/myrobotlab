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
ear = Runtime.createAndStart("ear", "WebkitSpeechRecognition")
######################################################################
# create the html filter to filter the output of program ab
######################################################################
htmlfilter = Runtime.createAndStart("htmlfilter", "HtmlFilter")
######################################################################
# the mouth
######################################################################
mouth = Runtime.createAndStart("mouth", "AcapelaSpeech")

mouth.addEar(ear)
# add a link between the webkit speech to publish to ProgramAB
ear.addTextListener(programab)
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
    if (numFound > 0):
      gloss = response.getResults().get(0).getFieldValue("sense_gloss")
      print "Found " + str(numFound)
      #mouth.speak(str(gloss) + "Yes, a " + str(x) + " is a " + str(y))
      programab.invoke("publishText", str(gloss) + "Yes, a " + str(x) + " is a " + str(y))
    else:
      programab.invoke("publishText","No")
      # mouth.speak("No")
    
def whatIsA(x):
    q = "sense_lemma:\"" + str(x.lower().replace(" ","_")) + "\""
    response = solr.search(q)
    numFound = response.getResults().size()
    if (numFound > 0):
      gloss = response.getResults().get(0).getFieldValue("sense_gloss")
      programab.invoke("publishText",gloss[0])
      # mouth.speak(str(gloss))
    
    
# lets try a traversal of subject + verb , pick first facet of object.
def xDoesYToZ(x, y):
    q = "+subject:\"" + str(x) + "\" +verb:\""+str(y)+"\""
    # TODO: pass a facet request in..   
    response = solr.search(q)
    numFound = response.getResults().size()
    if (numFound > 0):
        object = response.getResults().get(0).getFieldValue("object")
        programab.invoke("publishText",object[0])
             