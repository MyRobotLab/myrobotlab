#########################################
# VoskSpeechRecognition.py
# description: Offline speech recognition using Vosk
# categories: speech recognition, speech, sound
# more info @: http://myrobotlab.org/service/VoskSpeechRecognition
#########################################
# Docs: https://alphacephei.com/vosk/
# Models: https://alphacephei.com/vosk/models

ear = runtime.start("ear", "VoskSpeechRecognition")

# Optional mouth attach (suppresses listening while speaking)
# mouth = runtime.start("mouth", "MarySpeech")
# ear.attach(mouth)

# List known / recommended models (name → "Language — description")
print(ear.getModelCatalog())
# Structured list for UIs: name, locale, language, description, label
print(ear.getAvailableModels())

# Install a language model (downloaded once into data/VoskSpeechRecognition/models/)
# ear.installModel("vosk-model-small-en-us-0.15")

# Or pick language (installs default small model for that locale)
# ear.setLanguage("en-US")
# ear.setLanguage("de-DE")
# ear.setLanguage("fr-FR")

# Show what is already on disk
print(ear.getInstalledModels())

# Start offline listening (auto-downloads configured model if needed)
# ear.startListening()

def onText(text):
    print("heard:", text)

ear.addListener("publishText", "python", "onText")
