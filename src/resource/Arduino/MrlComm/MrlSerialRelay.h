#ifndef MrlSerialRelay_h
#define MrlSerialRelay_h

#include <Arduino.h>
#include "Device.h"

// ===== published sub-types based on device type begin ===
#define  MRL_IO_NOT_DEFINED 0
#define  MRL_IO_SERIAL_0  	1
#define  MRL_IO_SERIAL_1  	2
#define  MRL_IO_SERIAL_2  	3
#define  MRL_IO_SERIAL_3  	4
// ===== published sub-types based on device type begin ===

class MrlSerialRelay:public Device {
  private:
	byte serialPort;
	HardwareSerial* serial;
  public:
    MrlSerialRelay(int deviceId);
    ~MrlSerialRelay();
    bool attach(byte serialPort);
    void update();
    void write(const byte* data, byte dataSize);
};

#endif
