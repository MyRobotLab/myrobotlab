#ifndef MrlWS_h
#define MrlWS_h

#if defined(ESP8266)

#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>
#include <WebSocketsServer.h>
#include <Hash.h>
#include "ArduinoMsgCodec.h"

class MrlWS {
  private:
  ESP8266WiFiMulti WiFiMulti;
  WebSocketsServer* webSocket;
  unsigned char inputBuffer[MAX_MSG_SIZE];
  byte inputHead;
  byte inputTail;
  unsigned char num;
  String outBuffer;
  public:
  MrlWS(WebSocketsServer& wsServer);
  int available();
  void flush();
  void write(const unsigned char*, int);
  void write(unsigned char);
  unsigned char read();
  void webSocketEvent(unsigned char num, WStype_t type, unsigned char* payload, unsigned int lenght);
};
#endif
#endif

