#include "MrlIo.h"

int MrlIo::openIo = 0;

MrlIo::MrlIo() {
	ioType = MRL_IO_NOT_DEFINED;
}

MrlIo::~MrlIo(){
	//end();
}

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
	// ioType == _ioType; huh ?
	openIo |= (1 << ioType);
  //delay(500);
	return true;
}

void MrlIo::write(unsigned char value) {
	if(!(openIo & (1 << ioType))){
		//port close
		ioType = MRL_IO_NOT_DEFINED;
		return;
	}
// if(ioType==MRL_IO_SERIAL_0){
//  Serial.write(value);
// }
// else if(ioType==MRL_IO_SERIAL_1){
//  Serial1.write(value);
// }
	serial->write(value);
}

void MrlIo::write(unsigned char* buffer, int len) {
	if(!(openIo & (1 << ioType))){
		//port close
		ioType = MRL_IO_NOT_DEFINED;
		return;
	}
	serial->write(buffer, len);
}

int MrlIo::read() {
	if(!(openIo & (1 << ioType))){
		//port close
		ioType = MRL_IO_NOT_DEFINED;
		return -1;
	}
	return serial->read();
}

int MrlIo::available() {
  //serial->println(ioType);
	if(!(openIo & (1 << ioType))){
		//port close
		ioType = MRL_IO_NOT_DEFINED;
    
		return 0;
	}
	return serial->available();
}

void MrlIo::end() {
	if (!(openIo & (1 << ioType))){
		//port already close
		ioType = MRL_IO_NOT_DEFINED;
		return;
	}
	serial->end();
	serial = NULL;
	openIo &= ~(1 << ioType);
	ioType = MRL_IO_NOT_DEFINED;
	delay(500);
}

void MrlIo::flush() {
	if (!(openIo & (1 << ioType))){
		//port already close
		ioType = MRL_IO_NOT_DEFINED;
		return;
	}
	serial->flush();
}


