#include "MrlWS.h"
#include <Arduino.h>
#if defined(ESP8266)
#define USE_SERIAL Serial

MrlWS::MrlWS(WebSocketsServer& wsServer) {
  webSocket = &wsServer;
  inputHead = 0;
  inputTail = 0;
  
    USE_SERIAL.setDebugOutput(true);

    USE_SERIAL.println();
    USE_SERIAL.println();
    USE_SERIAL.println();

    for(uint8_t t = 4; t > 0; t--) {
        USE_SERIAL.printf("[SETUP] BOOT WAIT %d...\n", t);
        USE_SERIAL.flush();
        delay(1000);
}
WiFiMulti.addAP("SSID","PASS");
  while (WiFiMulti.run() != WL_CONNECTED) {
    delay(100);
  }
  webSocket->begin();
  USE_SERIAL.println("webSocketStarted");
}

void MrlWS::write(unsigned char b) {
  //unsigned char bu[1];
//  bu[0] = b;
//  write(bu,1);
  outBuffer.concat(String(b));
  outBuffer.concat("/");
}

void MrlWS::write(const unsigned char* b, int len) {
  for (int i = 0; i < len; i++) {
    outBuffer.concat(String(b[i]));
    outBuffer.concat("/");
  }
}

void MrlWS::flush() {
  webSocket->sendTXT(num,outBuffer);
  outBuffer =  "";
}

int MrlWS::available() {
  if (inputTail >= inputHead) {
    //USE_SERIAL.printf("inputHead: [%u] inputTail: [%u] available
    return inputTail - inputHead;
  }
  else {
    return MAX_MSG_SIZE - inputHead + inputTail;
  }
return 0;
}

unsigned char MrlWS::read() {
  //USE_SERIAL.print("read called");
  if (inputHead == inputTail) {
    return 0;
  }
  else {
    unsigned char retVal = inputBuffer[inputHead++];
    if (inputHead == MAX_MSG_SIZE) {
      inputHead = 0;
    }
    //USE_SERIAL.println(retVal);
    return retVal;
  }
return 0;
}

void MrlWS::webSocketEvent(unsigned char num, WStype_t type, unsigned char* payload, unsigned int lenght) {
  USE_SERIAL.print("in web Socket Event");
  this->num = num;
    switch(type) {
        case WStype_DISCONNECTED:
            USE_SERIAL.printf("[%u] Disconnected!\n", num);
            break;
        case WStype_CONNECTED:
            {
                IPAddress ip = webSocket->remoteIP(num);
                USE_SERIAL.printf("[%u] Connected from %d.%d.%d.%d url: %s\n", num, ip[0], ip[1], ip[2], ip[3], payload);
        
        // send message to client
        //webSocket->sendTXT(num, "Connected");
            }
            break;
        case WStype_TEXT:
            USE_SERIAL.printf("[%u] get Text: %s\n", num, payload);
            
            for (int i = 0; i < lenght; i++) {
              inputBuffer[inputTail++] = payload[i];
              USE_SERIAL.println(payload[i]);
              if (inputTail == MAX_MSG_SIZE) {
                inputTail = 0;
              }
              if (inputTail == inputHead) {
                break;
              }
            }
            
            // send message to client
            // webSocket->sendTXT(num, "message here");

            // send data to all connected clients
            // webSocket.broadcastTXT("message here");
            break;
        case WStype_BIN:
            USE_SERIAL.printf("[%u] get binary lenght: %u\n", num, lenght);
            hexdump(payload, lenght);

            // send message to client
            // webSocket.sendBIN(num, payload, lenght);
            break;
    }

}

#endif

