#ifndef MrlMsg_h
#define MrlMsg_h

#include "ArduinoMsgCodec.h"
#include "LinkedList.h"

#define MAX_MSG_SIZE 64

// ------ error types ------
#define ERROR_SERIAL            1
#define ERROR_UNKOWN_CMD        2
#define ERROR_ALREADY_EXISTS    3
#define ERROR_DOES_NOT_EXIST    4
#define ERROR_UNKOWN_SENSOR     5

/**********************************************************************
 * MrlMsg - This class is responsible to send the messages to MRL in
 *          java-land
 */
class MrlMsg{
  private:
    LinkedList<byte> dataBuffer;
    int type;  // message id ie PUBLISH_VERSION
    int deviceId;
    // those variable allow to count the data and add the number to the dataBuffer
    int dataSizePos;
    int dataSizeCount;
    bool dataCountEnabled;

  public:
    MrlMsg(int msgType){
      type=msgType;
      deviceId = -1;
      dataCountEnabled = false;
    }
  
    MrlMsg(int msgType, int id) {
      type=msgType;
      deviceId = id;
      dataCountEnabled = false;
    }
  
    // addData overload methods
    void addData(byte data) {
      dataBuffer.add(data);
      dataSizeCount++;
    }
  
    void addData(int data) {
      addData((byte)data);
    }
  
    void addData(unsigned int data) {
      addData((byte)data);
    }
  
    void addData(unsigned char* dataArray, int size, bool inclDataSize = false) {
      if (inclDataSize) {
        dataBuffer.add((byte)size);
      }
      for (int i = 0; i < size; i++){
        dataBuffer.add((byte)dataArray[i]);
      }
      dataSizeCount += size;
    }
  
    void addData(long data) {
      addData((unsigned long)data);
    }
  
    void addData(unsigned long data) {
      dataBuffer.add((byte)((data >> 24) & 0xFF));
      dataBuffer.add((byte)((data >> 16) & 0xFF));
      dataBuffer.add((byte)((data >> 8) & 0xFF));
      dataBuffer.add((byte)(data & 0xFF));
      dataSizeCount += 4;
    }
  
    void addData(String data, bool inclDataSize = false) {
      if (inclDataSize) {
        dataBuffer.add((byte)data.length());
      }
      for(unsigned int i=0; i < data.length(); i++){
        dataBuffer.add(data[i]);
      }
      dataSizeCount += data.length();
    }
    void addData(int data[], int size, bool inclDataSize = false) {
      if (inclDataSize) {
        dataBuffer.add((byte)size);
      }
      for(int i=0; i < size; i++){
        dataBuffer.add(data[i]);
      }
      dataSizeCount += size;
    }
    void addData16(int data) {
      addData16((unsigned int)data);
    }
    void addData16(unsigned int data) {
      dataBuffer.add((byte)(data >> 8));
      dataBuffer.add((byte)(data & 0xFF));
      dataSizeCount += 2;
    }
  
    // enabled the data counter
    void countData(){
      dataCountEnabled = true;
      dataSizeCount = 0;
      dataSizePos = dataBuffer.size();
    }
  
    // add the data counter at the saved position
    void addDataCount(){
      dataCountEnabled = false;
      dataBuffer.add(dataSizePos,(byte)dataSizeCount);
    }
  
    // send the message to Serial
    void sendMsg() {
      if (dataCountEnabled) {
        addDataCount();
      }
      int dataSize=dataBuffer.size();
      int msgSize=dataSize+1;
      if (deviceId > -1) {
        msgSize+=1;
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
};

#endif
