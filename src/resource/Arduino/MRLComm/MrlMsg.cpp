#include "MrlMsg.h"

MrlMsg::MrlMsg(int msgType) {
  type = msgType;
  deviceId = -1;
  dataCountEnabled = false;
  begin(MRL_IO_SERIAL_0,115200);
  auto_send=0;
}

MrlMsg::MrlMsg(int msgType, int id) {
  type = msgType;
  deviceId = id;
  dataCountEnabled = false;
  begin(MRL_IO_SERIAL_0,115200);
  auto_send=0;
}

// addData overload methods
void MrlMsg::addData(byte data) {
  dataBuffer.add(data);
  dataSizeCount++;
  if(dataBuffer.size() >= auto_send && auto_send) sendMsg();
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
  if(dataBuffer.size() >= auto_send && auto_send) sendMsg();
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
  if(dataBuffer.size() >= auto_send && auto_send) sendMsg();
}

void MrlMsg::addData(String data, bool inclDataSize) {
  if (inclDataSize) {
    dataBuffer.add((byte) data.length());
  }
  for (unsigned int i = 0; i < data.length(); i++) {
    dataBuffer.add(data[i]);
  }
  dataSizeCount += data.length();
  if(dataBuffer.size() >= auto_send && auto_send) sendMsg();
}

void MrlMsg::addData(int data[], int size, bool inclDataSize) {
  if (inclDataSize) {
    dataBuffer.add((byte) size);
  }
  for (int i = 0; i < size; i++) {
    dataBuffer.add(data[i]);
  }
  dataSizeCount += size;
  if(dataBuffer.size() >= auto_send && auto_send) sendMsg();
}

void MrlMsg::addData16(int data) {
  addData16((unsigned int) data);
}

void MrlMsg::addData16(unsigned int data) {
  dataBuffer.add((byte)(data >> 8));
  dataBuffer.add((byte)(data & 0xFF));
  dataSizeCount += 2;
  if(dataBuffer.size() >= auto_send && auto_send) sendMsg();
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
 // publishDebug("Sending msg");
  bool _dataCountEnabled = dataCountEnabled;
  if (dataCountEnabled) {
    addDataCount();
  }
  int dataSize = dataBuffer.size();
  int msgSize = dataSize + 1;
  if (deviceId > -1) {
    msgSize += 1;
  }
  write(MAGIC_NUMBER);
  write(msgSize);
  write(type);
  if (deviceId > -1) {
    write(deviceId);
  }
  ListNode<byte>* node = dataBuffer.getRoot();
  while (node != NULL) {
    write(node->data);
    node = node->next;
  }
  flush();
  dataBuffer.clear();
  if (_dataCountEnabled) {
    countData();
  }
}

/**
 * Publish Debug - return a text debug message back to the java based arduino service in MRL
 * MAGIC_NUMBER|1+MSG_LENGTH|MESSAGE_BYTES
 *
 * This method will publish a string back to the Arduino service for debugging purproses.
 *
 */

void MrlMsg::publishDebug(String message){
  // NOTE-KW:  If this method gets called excessively I have seen memory corruption in the
  // arduino where it seems to be getting a null string passed in as "message"
  // very very very very very odd..  I suspect a bug in the arduino hardware/software
  MrlMsg msg(PUBLISH_DEBUG);
  msg.addData(message);
  msg.sendMsg();
}

/**
 * send an error message/code back to MRL.
 * MAGIC_NUMBER|2|PUBLISH_MRLCOMM_ERROR|ERROR_CODE
 */
// KW: remove this, force an error message.
void MrlMsg::publishError(int type) {
  MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
  msg.addData(type);
  msg.sendMsg();
}
/**
 * Send an error message along with the error code
 *
 */
void MrlMsg::publishError(int type, String message) {
  MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
  msg.addData(type);
  msg.addData(message);
  msg.sendMsg();
}

void MrlMsg::autoSend(int value){
  auto_send = value;
}

