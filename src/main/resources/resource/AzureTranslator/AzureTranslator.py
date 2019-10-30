# AZURE TRANSLATOR DEMO
# this demo is minimal
# here you can see cooked things with voice change based on language 
# https://github.com/MyRobotLab/inmoov/blob/develop/InMoov/services/G_Translator.py



#voice service
mouth=Runtime.createAndStart("mouth", "MarySpeech")
#azure service
AzureTranslator=Runtime.createAndStart("AzureTranslator", "AzureTranslator")

AzureTranslator.setCredentials("YOUR_KEY_HERE_7da9defb-7d86-etc...")



#voice output
mouth.setVoice("cmu-bdl-hsmm")
mouth.setLanguage("en")

supported_languages = { # as defined here: http://msdn.microsoft.com/en-us/library/hh456380.aspx
  'da' : 'Danish',
  'nl' : 'Dutch',
  'en' : 'English',
  'fr' : 'French',
  'de' : 'German',
  'it' : 'Italian',
  'is' : 'Iceland',
  'no' : 'Norwegian',
  'pt' : 'Portuguese',
  'ru' : 'Russian',
  'es' : 'Spanish',
  'sv' : 'Swedish',
  'tr' : 'Turkish',
  'ro' : 'Romanian',
  'ja' : 'Japanese',
  'pl' : 'Polish',
}

#Mary tts voice name map 
male_languagesMary = { 
  'da' : 'cmu-bdl-hsmm',#'dfki-pavoque-neutral-hsmm',
  'nl' : 'cmu-bdl-hsmm',#'dfki-pavoque-neutral-hsmm',
  'en' : 'cmu-bdl-hsmm',
  'fr' : 'cmu-bdl-hsmm',
  'de' : 'cmu-bdl-hsmm',#'dfki-pavoque-neutral-hsmm',
  'it' : 'cmu-bdl-hsmm',#'istc-lucia-hsmm',
  'is' : 'cmu-bdl-hsmm',#'dfki-pavoque-neutral-hsmm',
  'no' : 'cmu-bdl-hsmm',#'dfki-pavoque-neutral-hsmm',
  'pt' : 'cmu-bdl-hsmm',#'istc-lucia-hsmm',
  'ru' : 'cmu-bdl-hsmm',
  'es' : 'cmu-bdl-hsmm',#'istc-lucia-hsmm',
  'sv' : 'cmu-bdl-hsmm',
  'tr' : 'cmu-bdl-hsmm',#'dfki-ot-hsmm',
  'ro' : 'cmu-bdl-hsmm',
  'ja' : 'cmu-bdl-hsmm',
  'pl' : 'cmu-bdl-hsmm',
}

#Translate to :
#TODO ADD TRANSLATED KEYWORDS
en_languages = {
  'danish' : 'da',
  'danois' : 'da',
  'dutch' : 'nl',
  'hollandais' : 'nl',
  'english' : 'en',
  'anglais' : 'en',
  'french' : 'fr',
  'français' : 'fr',
  'german' : 'de',
  'allemand' : 'de',
  'italian' : 'it',
  'italien' : 'it',
  'norwegian' : 'no',
  'norvegien' : 'no',
  'Icelandic' : 'is',
  'islandais' : 'is',
  'spanish' : 'es',
  'espagnol' : 'es',
  'swedish' : 'sv',
  'suédois' : 'sv',
  'japonese' : 'ja',
  'japonais' : 'ja',
  'portuguese' : 'pt',
  'portuguais' : 'pt',
  'turkish' : 'tr',
  'turk' : 'tr',
  'russian' : 'ru',
  'russe' : 'ru',
  'romanian' : 'ro',
  'roumain' : 'ro',  
}


#main function
def translateText(text,language):

	#AzureTranslator.fromLanguage('en')
	RealLang="0"
	try:
		RealLang=en_languages[language]
	except: 
		mouth.speakBlocking("I dont know this language, i am so sorry, or you made a mistake dude")
	print RealLang
	
	try:
		AzureTranslator.detectLanguage(text)
	except:
		mouth.speakBlocking("Check your azure credentials please ! I can't do all the work for you, i am just a robot")
		RealLang="0"
	
	
	if RealLang!="0":
		AzureTranslator.toLanguage(RealLang)
		sleep(0.5)
		t_text=AzureTranslator.translate(text)   
		
		#small trick to prevent old api connection problems
		i=0
		while 'Cannot find an active Azure Market Place' in t_text and i<50: 
			print(i,t_text)
			i += 1 
			sleep(0.2)
			AzureTranslator.detectLanguage(text)
			t_text=AzureTranslator.translate(text+" ")


		if 'Cannot find an active Azure Market Place' in t_text:
			mouth.speakBlocking("There is a problem with azure, i am so sorry. Or maybe I am tired")
		else:
			# change voice to map language
			mouth.setVoice(male_languagesMary[RealLang])  
			mouth.speakBlocking(t_text)
			# Go back original voice
			mouth.setVoice("cmu-bdl-hsmm")

# translateText(THE TEXT,TO LANGUAGE ( from #Translate to : )			
translateText(u"Hola buenos dias","french")
sleep(2)
translateText(u"Hello ! and I can translate so many languages ! ","italian")
