#ifndef MrlCmd_h
#define MrlCmd_h

#include "ArduinoMsgCodec.h"
#include "LinkedList.h"
#include "MrlIo.h"
#include "MrlMsg.h"

#define MAX_MSG_SIZE 64

class MrlCmd : public MrlIo{
private:
	unsigned char ioCmd[MAX_MSG_SIZE];
	int byteCount;
	int msgSize;
public:
	MrlCmd(int ioType);
	~MrlCmd();
	bool readCommand();
	unsigned char* getIoCmd();
	unsigned char getIoCmd(int pos);
	int getMsgSize();
};

#endif
