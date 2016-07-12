#ifndef MrlNeopixel_h
#define MrlNeopixel_h

#include "Device.h"
#include "MrlMsg.h"

/***********************************************************************
 * NEOPIXEL DEFINE
 */
#define PIXEL_PORT PORTC
#define PIXEL_DDR DDRC
//timing for the neopixel communication
#define T1H 900    
#define T1L 600
#define T0H 400
#define T0L 900
#define RES 6000

#define NS_PER_SEC (1000000000L)
#define CYCLES_PER_SEC (F_CPU)
#define NS_PER_CYCLE ( NS_PER_SEC / CYCLES_PER_SEC )
#define NS_TO_CYCLES(n) ( (n) / NS_PER_CYCLE )

/*****************************
 * Neopixel device
 * 
 * adapted from https://github.com/bigjosh/SimpleNeoPixelDemo/blob/master/SimpleNeopixelDemo/SimpleNeopixelDemo.ino
 * it contains board specific code
 * so far only working on pins 30-37 on Mega
 * TODO: support on more pins and on UNO
 */
struct Pixel{
  unsigned char red;
  unsigned char blue;
  unsigned char green;
  Pixel();
};

class MrlNeopixel:public Device{
  private:
    unsigned int numPixel;  
    Pixel* pixels;
    uint8_t bitmask;
    unsigned long lastShow;
    bool newData;
  public:
  MrlNeopixel():Device(DEVICE_TYPE_NEOPIXEL){};
  ~MrlNeopixel();
  bool deviceAttach(unsigned char config[], int configSize);
  inline void sendBit(bool bitVal);
  inline void sendByte(unsigned char byte);
  inline void sendPixel(Pixel p);
  void show();
  void neopixelWriteMatrix(unsigned char* ioCmd);
  void update();
};



#endif
