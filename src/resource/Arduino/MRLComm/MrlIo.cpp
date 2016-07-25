#include "MrlIo.h"

int MrlIo::openIo = 0;

MrlIo::MrlIo() {
	ioType = MRL_IO_NOT_DEFINED;
}

MrlIo::~MrlIo(){
	//end();
}

bool MrlIo::begin(int ioType, long speed) {
	if (openIo & (1 << ioType)){
		//port already open
		this->ioType = ioType;
		return true;
	}
	switch (ioType){
		case MRL_IO_SERIAL_0:
			serial = &Serial;
			//Serial.begin(speed);
			break;
#if BOARD == BOARD_TYPE_MEGA
		case MRL_IO_SERIAL_1:
			serial = &Serial1;
			//Serial1.begin(speed);
			break;
		case MRL_IO_SERIAL_2:
			serial = &Serial2;
			//Serial2.begin(speed);
			break;
		case MRL_IO_SERIAL_3:
			serial = &Serial3;
			//Serial3.begin(speed);
			break;
#endif
		default:
			return false;
			break;
	}
	serial->begin(speed);
	this->ioType == ioType;
	openIo |= (1 << ioType);
	return true;
}

void MrlIo::write(unsigned char value) {
	if(!(openIo & (1 << ioType))){
		//port close
		ioType = MRL_IO_NOT_DEFINED;
		return;
	}
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
