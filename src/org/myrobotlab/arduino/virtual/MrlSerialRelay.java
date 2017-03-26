package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.VirtualMsg;

public class MrlSerialRelay extends Device {
  
//===== published sub-types based on device type begin ===
public final int  MRL_IO_NOT_DEFINED  = 0;
public final int  MRL_IO_SERIAL_0 =   1;
public final int  MRL_IO_SERIAL_1 =   2;
public final int  MRL_IO_SERIAL_2 =   3;
public final int  MRL_IO_SERIAL_3 =   4;
//===== published sub-types based on device type begin ===

 int serialPort;
 HardwareSerial serial;
 
  public MrlSerialRelay(int deviceId) {
    super(deviceId);
    this.type = Msg.DEVICE_TYPE_SERIAL;
  }
  
  boolean attach(int serialPort){
    // msg->publishDebug("MrlSerialRelay.deviceAttach !");
    this.serialPort = serialPort;
    switch(serialPort){
    case MRL_IO_SERIAL_0:
      // serial = &Serial;
      break;
  // #if defined(ARDUINO_AVR_MEGA2560) || defined(ARDUINO_AVR_ADK)
    case MRL_IO_SERIAL_1:
      // serial = &Serial1;
      break;
    case MRL_IO_SERIAL_2:
      // serial = &Serial2;
      break;
    case MRL_IO_SERIAL_3:
     // serial = &Serial3;
      break;
  // #endif
    default:
      return false;
    }
    serial.begin(115200);
    return true;
  }

  void write(int data, int dataSize){
    
  }

  public void update(){
    int[] buffer = new int[VirtualMsg.MAX_MSG_SIZE];
    int pos=0;
    if(serial.available()){
      //msg->publishDebug("data available");
      while(serial.available()){
        buffer[pos++] = serial.read();
      }
      msg.publishSerialData(id,buffer);
    }

}

  public void write(int[] data, int dataSize) {
    serial.write(data, dataSize);
  }
}

