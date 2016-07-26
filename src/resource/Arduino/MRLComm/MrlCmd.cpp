#include "MrlCmd.h"

MrlCmd::MrlCmd(int ioType){
	begin(ioType,115200);
	byteCount=0;
	msgSize=0;
}

MrlCmd::~MrlCmd(){

}

/**
 * getCommand() - This is the main method to read new data from the serial port,
 * when a full mrlcomm message is read from the serial port.
 * return values: true if the serial port read a full mrlcomm command
 *                false if the serial port is still waiting on a command.
 */

bool MrlCmd::readCommand(){
	// handle serial data begin
	int bytesAvailable = available();
	if (bytesAvailable > 0) {
		//MrlMsg::publishDebug("RXBUFF:" + String(bytesAvailable));
		// now we should loop over the available bytes .. not just read one by one.
		for (int i = 0; i < bytesAvailable; i++) {
			// read the incoming byte:
			unsigned char newByte = read();
			//MrlMsg::publishDebug("RX:" + String(newByte));
			++byteCount;
			// checking first byte - beginning of message?
			if (byteCount == 1 && newByte != MAGIC_NUMBER) {
				MrlMsg::publishError(ERROR_SERIAL);
				// reset - try again
				byteCount = 0;
				// return false;
			}
			if (byteCount == 2) {
				// get the size of message
				// todo check msg < 64 (MAX_MSG_SIZE)
				if (newByte > 64) {
					// TODO - send error back
					byteCount = 0;
					continue; // GroG - I guess  we continue now vs return false on error conditions?
				}
				msgSize = newByte;
			}
			if (byteCount > 2) {
				// fill in msg data - (2) headbytes -1 (offset)
				ioCmd[byteCount - 3] = newByte;
			}
			// if received header + msg
			if (byteCount == 2 + msgSize) {
				// we've reach the end of the command, just return true .. we've got it
				byteCount = 0;
				return true;
			}
		}
	} // if Serial.available
	  // we only partially read a command.  (or nothing at all.)
	return false;
}


unsigned char* MrlCmd::getIoCmd() {
	return ioCmd;
}

unsigned char MrlCmd::getIoCmd(int pos){
	return ioCmd[pos];
}

int MrlCmd::getMsgSize(){
	return msgSize;
}
