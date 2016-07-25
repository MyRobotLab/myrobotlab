#ifndef MrlIo_h
#define MrlIo_h

#include <Arduino.h>
#include "ArduinoMsgCodec.h"

#define MRL_IO_NOT_DEFINED 0
#define MRL_IO_SERIAL_0 1
#if BOARD==BOARD_TYPE_MEGA
	#define MRL_IO_SERIAL_1 2
	#define MRL_IO_SERIAL_2 3
	#define MRL_IO_SERIAL_3 4
#endif


class MrlIo {
	private:
		int ioType;
		HardwareSerial* serial;
	public:
		static int openIo;
		MrlIo();
		~MrlIo();
		bool begin(int ioType, long speed);
		void write(unsigned char value);
		void write(unsigned char* buffer, int len);
		int read();
		int available();
		void end();
		void flush();
   void test();
};

#endif
