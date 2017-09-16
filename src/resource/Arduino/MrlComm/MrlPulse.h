#ifndef MrlPulse_h
#define MrlPulse_h

/**
 * Pulse Sensor Device
 * TODO: add a description about this device! How does it work? What is it?
 */
class MrlPulse : public Device {
  public:
    unsigned long lastValue;
    int count;
    Pin* pin;
    MrlPulse() : Device(SENSOR_TYPE_PULSE) {
      count=0;
    }
    ~MrlPulse() {
      if (pin) delete pin;
    }
    void pulse(unsigned char* ioCmd) {
      if (!pin) return;
      pin->count = 0;
      pin->target = toLong(ioCmd,2);
      pin->rate = ioCmd[6];
      pin->rateModulus = ioCmd[7];
      pin->state = PUBLISH_SENSOR_DATA;
    }
    void pulseStop() {
      pin->state = PUBLISH_PULSE_STOP;
    }
    void update() {
      //this need work
      if (type == 0) return;
      lastValue = (lastValue == 0) ? 1 : 0;
      // leading edge ... 0 to 1
      if (lastValue == 1) {
        count++;
        if (pin->count >= pin->target) {
          pin->state = PUBLISH_PULSE_STOP;
        }
      }
      // change state of pin
      digitalWrite(pin->address, lastValue);
      // move counter/current position
      // see if feedback rate is valid
      // if time to send feedback do it
      // if (loopCount%feedbackRate == 0)
      // 0--to-->1 counting leading edge only
      // pin.method == PUBLISH_PULSE_PIN &&
      // stopped on the leading edge
      if (pin->state != PUBLISH_PULSE_STOP && lastValue == 1) {
        // TODO: support publish pulse stop!
        // publishPulseStop(pin.state, pin.sensorIndex, pin.address, sensor.count);
        // deactivate
        // lastDebounceTime[digitalReadPin[i]] = millis();
        // test git
      }
      if (pin->state == PUBLISH_PULSE_STOP) {
        //pin.isActive = false;
        // TODO: support publish pulse stop!
        // TODO: if we're not active, remove ourselves from the device list?
      }
      // publish the pulse!
      // TODO: support publishPuse!
      // publishPulse(pin.state, pin.sensorIndex, pin.address, pin.count);
    }

};

#endif
