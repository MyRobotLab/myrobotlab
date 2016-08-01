#include "MrlIo.h"

int MrlIo::openIo = 0;

MrlIo::MrlIo() {
	ioType = MRL_IO_NOT_DEFINED;
}

MrlIo::~MrlIo(){
	//end();
}
/***
 * begin() method will select wich serial port (other comm port) the derived class will use
 */
bool MrlIo::begin(int _ioType, long speed) {
	if (openIo & (1 << _ioType)){
		//port already open
		ioType = _ioType;
		return true;
	}
	switch (_ioType){
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
			break;
	}
	serial->begin(speed);
	ioType = _ioType;
	openIo |= (1 << ioType);
	return true;
}

void MrlIo::write(unsigned char value) {
  if (!checkOpenPort()) return;
	serial->write(value);
}

void MrlIo::write(unsigned char* buffer, int len) {
  if (!checkOpenPort()) return;
	serial->write(buffer, len);
}

int MrlIo::read() {
  if (!checkOpenPort()) return -1;
	return serial->read();
}

int MrlIo::available() {
  if (!checkOpenPort()) return 0;
	return serial->available();
}

void MrlIo::end() {
  if (!checkOpenPort()) return;
	serial->end();
	serial = NULL;
	openIo &= ~(1 << ioType);
	ioType = MRL_IO_NOT_DEFINED;
	delay(500);
}

void MrlIo::flush() {
  if (!checkOpenPort()) return;
	if (!(openIo & (1 << ioType))){
		//port already close
		ioType = MRL_IO_NOT_DEFINED;
		return;
	}
	serial->flush();
}

bool MrlIo::checkOpenPort(){
  if (!(openIo & (1 << ioType))){
    //port already close
    ioType = MRL_IO_NOT_DEFINED;
    return false;
  }
  return true;
}

