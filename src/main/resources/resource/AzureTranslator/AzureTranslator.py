#########################################
# AzureTranslator.py
# description: language translation service
# more info @: http://myrobotlab.org/service/AzureTranslator
#########################################
# you will need a azure translator setup 
# this will guid you through the process
# https://docs.microsoft.com/en-us/azure/cognitive-services/translator/quickstart-translator?tabs=csharp

# we will connect the following services together
python = runtime.start("python", "Python")
brain = runtime.start("brain", "ProgramAB")
out_translator = runtime.start("out_translator", "AzureTranslator")
mouth = runtime.start("mouth", "MarySpeech")

# lets set blocking on the speech
mouth.setBlocking(False)
mouth.setLanguage("en")

# load your key here - only need to do it once
# then remove this line completely to keep it secure
# out_translator.setKey("xxxxxxxxxxxxxxxxxxxxxxxx")
out_translator.setLocation("eastus")
out_translator.setFrom("en")
out_translator.setTo("en")


# attach the mouth to the out_translator
mouth.attach(out_translator)

# set the mouth to an appropriate language or voice
# mouth.setVoice('Pierre')

def simple_translate(lang, text):
    print('simple_translate ' + lang + ' ' + text)    
    out_translator.setTo(lang)
    # switching voice for mary speech can take a very long time :(
    mouth.setLanguage(lang) 
    voice_name = mouth.getVoice().name
    translated = out_translator.translate('now in ' + mouth.getVoice().getLocale().getDisplayLanguage() + ', my name is ' + voice_name + ', ' + text)
    print(voice_name + ' translated to ' + translated)
    sleep(1)

text = "Hello ! let's  make  some robots today !"
# mouth.speak('i will translate ' + text)
simple_translate('en', text)
simple_translate('fr', text)
simple_translate('it', text)
simple_translate('de', text)

# lets connect the out_translator to the brain
# the brain will listen to keyboard input and when
# it publishes a response, the response will be sent to the
# out_translator
brain.attachTextListener(out_translator)
brain.startSession('GroG','Alice')

# we'll set our mouth and out_translator to french
out_translator.setTo("fr")
mouth.setLanguage("fr")

# setup a callback that gets the translated response
def on_translated(text):
    print('translated response is ' + text)
    
python.subscribe('out_translator', 'publishText', 'python', 'on_translated')   

# now we can talk to the brain in english and it will respond in french
english_response = brain.getResponse("hello, how are you?")
print('non translated response is ' + str(english_response))

english_response = brain.getResponse("what can you do?")
print('non translated response is ' + str(english_response))

english_response = brain.getResponse("what time is it?")
print('non translated response is ' + str(english_response))


# create a new translator for incoming text
# we will detect language and translate to english
in_translator = runtime.start("in_translator", "AzureTranslator")
in_translator.setDetect(True)
in_translator.setTo("en")

# attach the incoming translator to the brain
brain.attachTextPublisher(in_translator)

# subscribe to language detection
python.subscribe('in_translator', 'publishDetectedLanguage')

# Dynamically switching languages based on detected input
# when a language is detected we automatically
# switch our voice and translate "to" setting
# so if the bot is asked in french a question - it
# should reply in french, if asked in italian it
# will reply in italian, but all languages are
# using the same english aiml
def onDetectedLanguage(lang):
    # detect incoming language and
    # set appropriate response voice
    print('setting mouth voice to ' + lang)
    mouth.setLanguage(lang)
    print('setting out_translator to ' + lang)
    out_translator.setTo(lang)

# now that we have an incoming translator detecting
english_response = in_translator.translate("Où habitez-vous?")
print('in translated response is ' + str(english_response))

sleep(5)

# now that we have an incoming translator detecting
english_response = in_translator.translate("cosa sai fare?")
print('in translated response is ' + str(english_response))

sleep(5)

# now that we have an incoming translator detecting
english_response = in_translator.translate("what can you do?")
print('in translated response is ' + str(english_response))



