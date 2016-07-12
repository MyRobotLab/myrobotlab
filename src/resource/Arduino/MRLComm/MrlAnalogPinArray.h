#ifndef MrlAnalogPinArray_h
#define MrlAnalogPinArray_h

#include "Device.h"
#include "LinkedList.h"
#include "MrlMsg.h"

/**
 * Analog Pin Array Sensor
 * This represents a set of pins that can be sampled
 */
class MrlAnalogPinArray : public Device {
  // int pins[];
  public:
    LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0
    MrlAnalogPinArray() : Device(SENSOR_TYPE_ANALOG_PIN_ARRAY) {
  // specific stuffs for analog pins (sample rates?)
    }
    ~MrlAnalogPinArray() {
      while(pins.size()>0){
        delete pins.pop();
      }

    }
    // TODO: remove the direct write to the serial here..  devices shouldn't 
    // have a direct handle to the serial port.
    void update() {
      if (pins.size() > 0) {
        Serial.write(MAGIC_NUMBER);
        Serial.write(2 + pins.size() * 2);
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(id);
        Serial.write(pins.size() * 2); // size of sensor data
        for (int i = 0; i < pins.size(); ++i) {
          Pin* pin = pins.get(i);
          // TODO: moe the analog read outside of thie method and pass it in!
          pin->value = analogRead(pin->address);
          Serial.write(pin->value >> 8);   // MSB
          Serial.write(pin->value & 0xff); // LSB
        }
        Serial.flush();
      }
    }
};

#endif
