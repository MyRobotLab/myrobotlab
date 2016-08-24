#ifndef MrlMsg_h
#define MrlMsg_h

#include "ArduinoMsgCodec.h"
#include "LinkedList.h"
#include "MrlIo.h"


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
class MrlMsg : public MrlIo{
  private:
    LinkedList<byte> dataBuffer;
    int type;  // message id ie PUBLISH_VERSION
    int deviceId;
    // those variable allow to count the data and add the number to the dataBuffer
    int dataSizePos;
    int dataSizeCount;
    bool dataCountEnabled;
    int auto_send;

  public:
    MrlMsg(int msgType);
    MrlMsg(int msgType, int id);
    void addData(byte data);
    void addData(int data);
    void addData(unsigned int data);
    void addData(unsigned char* dataArray, int size, bool inclDataSize = false);
    void addData(long data);
    void addData(unsigned long data);
    void addData(String data, bool inclDataSize = false);
    void addData(int data[], int size, bool inclDataSize = false);
    void addData16(int data);
    void addData16(unsigned int data);
    void countData();
    void addDataCount();
    void sendMsg(); 
    void autoSend(int value);
    static long toInt(unsigned char* buffer, int start) {
          return (buffer[start] << 8) + buffer[start + 1];
        }

    static long toLong(unsigned char* buffer, int start) {
      return (((long)buffer[start] << 24) +
                 ((long)buffer[start + 1] << 16) +
                 (buffer[start + 2] << 8) + buffer[start + 3]);
       }
    static void publishDebug(String message);
    static void publishError(int type);
    static void publishError(int type, String message);
};

#endif
