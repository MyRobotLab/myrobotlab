#ifndef Pin_h
#define Pin_h

// ECHO FINITE STATE MACHINE - NON BLOCKING PULSIN
#define ECHO_STATE_START                    1
#define ECHO_STATE_TRIG_PULSE_BEGIN         2
#define ECHO_STATE_TRIG_PULSE_END           3
#define ECHO_STATE_MIN_PAUSE_PRE_LISTENING  4
#define ECHO_STATE_LISTENING                5
#define ECHO_STATE_GOOD_RANGE               6
#define ECHO_STATE_TIMEOUT                  7


// Pin Types must be in sync
// with Arduino.getMrlPinType

#define DIGITAL			    0
#define ANALOG			    1


/***********************************************************************
 * PIN - This class represents one of the pins on the arduino and it's
 * status in MRLComm.
 */
class Pin {
  public:
    int type; // might be useful in control
    int address; // pin #
    int value;
    // int state; // state of the pin - not sure if needed - reading | writing | some other state ? - dont add  it until necessary
    // int readModulus; // rate of reading or publish sensor data
    int debounce; // long lastDebounceTime - minDebounceTime
    // number of reads ?
    unsigned long target;
    // TODO: review/remove move the following members
    // to support pulse.
    int count;
    // remove me
    unsigned int rate;
    unsigned long lastUpdate;
    // remove me, needed for pulse
    // int rateModulus;

    // default constructor for a pin?
    // at a minimum we need type and address A0 D4 etc...
    Pin(int addr, int t, unsigned int rate) {
      type = t;
      address = addr;
      this->rate = rate;
    };
};

#endif
