runtime.createAndStart("uno","Arduino")
uno.setBoard("uno")
uno.connect("COM16")

uno.analogReadPollingStart(16)
uno.addListener("python", "publishPin")

def publishPin():
	dataPin = msg_uno_publishPin.data[0]
	print dataPin.pin, dataPin.value

sleep (3)
uno.analogReadPollingStop(16)
