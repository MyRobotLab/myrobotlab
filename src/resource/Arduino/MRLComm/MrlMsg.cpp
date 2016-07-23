#include "MrlMsg.h"

MrlMsg::MrlMsg(int msgType) {
	type = msgType;
	deviceId = -1;
	dataCountEnabled = false;
}

MrlMsg::MrlMsg(int msgType, int id) {
	type = msgType;
	deviceId = id;
	dataCountEnabled = false;
}

// addData overload methods
void MrlMsg::addData(byte data) {
	dataBuffer.add(data);
	dataSizeCount++;
}

void MrlMsg::addData(int data) {
	addData((byte) data);
}

void MrlMsg::addData(unsigned int data) {
	addData((byte) data);
}

void MrlMsg::addData(unsigned char* dataArray, int size, bool inclDataSize) {
	if (inclDataSize) {
		dataBuffer.add((byte) size);
	}
	for (int i = 0; i < size; i++) {
		dataBuffer.add((byte) dataArray[i]);
	}
	dataSizeCount += size;
}

void MrlMsg::addData(long data) {
	addData((unsigned long) data);
}

void MrlMsg::addData(unsigned long data) {
	dataBuffer.add((byte)((data >> 24) & 0xFF));
	dataBuffer.add((byte)((data >> 16) & 0xFF));
	dataBuffer.add((byte)((data >> 8) & 0xFF));
	dataBuffer.add((byte)(data & 0xFF));
	dataSizeCount += 4;
}

void MrlMsg::addData(String data, bool inclDataSize) {
	if (inclDataSize) {
		dataBuffer.add((byte) data.length());
	}
	for (unsigned int i = 0; i < data.length(); i++) {
		dataBuffer.add(data[i]);
	}
	dataSizeCount += data.length();
}

void MrlMsg::addData(int data[], int size, bool inclDataSize) {
	if (inclDataSize) {
		dataBuffer.add((byte) size);
	}
	for (int i = 0; i < size; i++) {
		dataBuffer.add(data[i]);
	}
	dataSizeCount += size;
}

void MrlMsg::addData16(int data) {
	addData16((unsigned int) data);
}

void MrlMsg::addData16(unsigned int data) {
	dataBuffer.add((byte)(data >> 8));
	dataBuffer.add((byte)(data & 0xFF));
	dataSizeCount += 2;
}

// enabled the data counter
void MrlMsg::countData() {
	dataCountEnabled = true;
	dataSizeCount = 0;
	dataSizePos = dataBuffer.size();
}

// add the data counter at the saved position
void MrlMsg::addDataCount() {
	dataCountEnabled = false;
	dataBuffer.add(dataSizePos, (byte) dataSizeCount);
}

// send the message to Serial
void MrlMsg::sendMsg() {
	if (dataCountEnabled) {
		addDataCount();
	}
	int dataSize = dataBuffer.size();
	int msgSize = dataSize + 1;
	if (deviceId > -1) {
		msgSize += 1;
	}
	Serial.write(MAGIC_NUMBER);
	Serial.write(msgSize);
	Serial.write(type);
	if (deviceId > -1) {
		Serial.write(deviceId);
	}
	ListNode<byte>* node = dataBuffer.getRoot();
	while (node != NULL) {
		Serial.write(node->data);
		node = node->next;
	}
	Serial.flush();
}

