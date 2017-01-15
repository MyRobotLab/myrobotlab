package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.VirtualMsg;

public abstract class Device {
  
  // deviceId is supplied by a parameter in an 'attach' message
  // deviceType is supplied by the device class as a form of runtime
  // class identification (rtti)
    Device(int deviceId, int deviceType){
      this.id = deviceId;
      this.type = deviceType;
    }
    // virtual ~Device(){
      // default destructor for the device class. 
      // destructor is set as virtual to call the destructor of the subclass. 
      // destructor should be done in the subclass level
    // }

    public Device(int deviceId) {
      this.id = deviceId & 0xFF;
    }
    
    public int id; // the all important id of the sensor - equivalent to the "name" - used in callbacks
    public int type; // what type of device is this?
    int state; // state - single at the moment to handle all the finite states of the sensors (todo maybe this moves into the subclasses?)
    abstract void update(); // all devices must implement this to update their state.

    VirtualMsg msg; // Msg is the generated interface for all communication
}
