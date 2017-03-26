#include "Msg.h"
#include "Device.h"
#include "MrlSerialRelay.h"
//#include "MrlComm.h"

MrlSerialRelay::MrlSerialRelay(int deviceId) : Device(deviceId, DEVICE_TYPE_SERIAL) {
	serialPort = MRL_IO_NOT_DEFINED;
}

MrlSerialRelay::~MrlSerialRelay() {
}

bool MrlSerialRelay::attach(byte serialPort){
  // msg->publishDebug("MrlSerialRelay.deviceAttach !");
  this->serialPort = serialPort;
  switch(serialPort){
  case MRL_IO_SERIAL_0:
	  serial = &Serial;
	  break;
#if defined(ARDUINO_AVR_MEGA2560) || defined(ARDUINO_AVR_ADK)
  case MRL_IO_SERIAL_1:
	  serial = &Serial1;
	  break;
  case MRL_IO_SERIAL_2:
	  serial = &Serial2;
	  break;
  case MRL_IO_SERIAL_3:
	  serial = &Serial3;
	  break;
#endif
  default:
	  return false;
  }
  serial->begin(115200);
  return true;
}

void MrlSerialRelay::write(const byte* data, byte dataSize){
	serial->write(data, dataSize);
}

void MrlSerialRelay::update(){
	byte buffer[MAX_MSG_SIZE];
	byte pos=0;
	if(serial->available()){
    //msg->publishDebug("data available");
		while(serial->available()){
			buffer[pos++] = serial->read();
		}
		msg->publishSerialData(id,buffer,pos);
	}
}
