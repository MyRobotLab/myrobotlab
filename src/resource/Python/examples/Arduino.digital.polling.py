# create and start an Arduino
arduino = Runtime.createAndStart("arduino","Arduino")

# connect the arduino to the appropriate port
arduino.connect("COM12")
# set the sampling rate 1 = fastest - 32767 = slowest
arduino.setSampleRate(8000)
# begin polling a digital line - sets mode to input
# and reads at sample rate modulus 
arduino.digitalReadPollStart(8)

# optional software debounce is available
# arduino.digitalDebounceOn()

# optional digital trigger only 
# true - will send back data *ONLY* on state change
# false - will continously send data based on sample rate - default
# arduino.setDigitalTriggerOnly(True)

# add this python service as a listener to arduino.publishPin ---> to be sent to 
# the "input()" method
arduino.addListener("publishPin", python.getName(), "input"); 

def input():
    # print 'python object is ', msg_clock_pulse
    pin = msg_arduino_publishPin.data[0]
    print 'pin data is ', pin.pin, pin.value
