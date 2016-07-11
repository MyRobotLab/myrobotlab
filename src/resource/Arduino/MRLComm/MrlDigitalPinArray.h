#ifndef MrlDigitalPinArray_h
#define MrlDigitalPinArray_h

#include "Device.h"
#include "MrlMsg.h"
#include "Pin.h"
#include "LinkedList.h"


/**
 * Digital Pin Array Sensor
 */
class MrlDigitalPinArray : public Device {
  public:
    LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0
    // config such as if they should publish the value in every loop
    // or only on a state change.
    MrlDigitalPinArray() : Device(SENSOR_TYPE_DIGITAL_PIN_ARRAY) {
    //pins = sensorPins;
    //for (int i = 0; i < pins.size(); i++) {
      // TODO: is this Analog or Digital?
    //  pinMode(i, INPUT);
    //}
    }
    ~MrlDigitalPinArray() {
      while(pins.size()>0){
        delete pins.pop();
      }

    }
    // devices shouldn't have a direct handle to the serial port..
    void update(unsigned long lastMicros) {
      if (pins.size() > 0) {
        Serial.write(MAGIC_NUMBER);
        Serial.write(2 + pins.size() * 1);
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(id);
        Serial.write(pins.size() * 1); // size of sensor data
        for (int i = 0; i < pins.size(); ++i) {
          Pin* pin = pins.get(i);
          // TODO: moe the digital read outside of thie method and pass it in!
          pin->value = digitalRead(pin->address);
          Serial.write(pin->value & 0xff); // LSB
        }
        Serial.flush();
      }
    }
};

#endif
