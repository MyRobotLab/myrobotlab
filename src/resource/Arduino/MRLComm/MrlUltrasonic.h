#ifndef MrlUltrasonic_h
#define MrlUltrasonic_h

#include "Device.h"
#include "LinkedList.h"
#include "MrlMsg.h"

/**
 * Ultrasonic Sensor
 * TODO: add a description about this device, what is it? what does it do?
 * How does it work?
 */
class MrlUltrasonic : public Device {
  public:
    int trigPin;
    int echoPin;
    LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0
    unsigned long ts;
    unsigned long lastValue;
    int timeoutUS;
    MrlUltrasonic() : Device(SENSOR_TYPE_ULTRASONIC) {
      timeoutUS=0; //this need to be set
      trigPin=0;//this need to be set
      echoPin=0;//this need to be set
    }
    ~MrlUltrasonic() {
      while(pins.size()>0){
        delete pins.pop();
      }

    }
    void update() {
      //This need to be reworked
      if (type == 0) return;
      Pin* pin = pins.get(0);
      if (pin->state == ECHO_STATE_START) {
        // trigPin prepare - start low for an
        // upcoming high pulse
        pinMode(trigPin, OUTPUT);
        digitalWrite(trigPin, LOW);
        // put the echopin into a high state
        // is this necessary ???
        pinMode(echoPin, OUTPUT);
        digitalWrite(echoPin, HIGH);
        unsigned long newts = micros();
        if (newts - ts > 2) {
          ts = newts;
          pin->state = ECHO_STATE_TRIG_PULSE_BEGIN;
        }
      } else if (pin->state == ECHO_STATE_TRIG_PULSE_BEGIN) {
        // begin high pulse for at least 10 us
        pinMode(trigPin, OUTPUT);
        digitalWrite(trigPin, HIGH);
        unsigned long newts = micros();
        if (newts - ts > 10) {
          ts = newts;
          pin->state = ECHO_STATE_TRIG_PULSE_END;
        }
      } else if (pin->state == ECHO_STATE_TRIG_PULSE_END) {
        // end of pulse
        pinMode(trigPin, OUTPUT);
        digitalWrite(trigPin, LOW);
        pin->state = ECHO_STATE_MIN_PAUSE_PRE_LISTENING;
        ts = micros();
      } else if (pin->state == ECHO_STATE_MIN_PAUSE_PRE_LISTENING) {
        unsigned long newts = micros();
        if (newts - ts > 1500) {
          ts = newts;
          // putting echo pin into listen mode
          pinMode(echoPin, OUTPUT);
          digitalWrite(echoPin, HIGH);
          pinMode(echoPin, INPUT);
          pin->state = ECHO_STATE_LISTENING;
        }
      } else if (pin->state == ECHO_STATE_LISTENING) {
        // timeout or change states..
        int value = digitalRead(echoPin);
        unsigned long newts = micros();
        if (value == LOW) {
          lastValue = newts - ts;
          ts = newts;
          pin->state = ECHO_STATE_GOOD_RANGE;
        } else if (newts - ts > timeoutUS) {
          pin->state = ECHO_STATE_TIMEOUT;
          ts = newts;
          lastValue = 0;
        }
      } else if (pin->state == ECHO_STATE_GOOD_RANGE || pin->state == ECHO_STATE_TIMEOUT) {
        // publishSensorDataLong(pin.address, sensor.lastValue);
        pin->state = ECHO_STATE_START;
      } // end else if
    }
};


#endif
