google = runtime.start('google','GoogleSearch')
python.subscribe('google', 'publishResults')

def onResults(data):
    print(data)
    
google.search('what is a goat?')
