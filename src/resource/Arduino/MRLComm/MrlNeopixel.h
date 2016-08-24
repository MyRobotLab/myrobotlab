#ifndef MrlNeopixel_h
#define MrlNeopixel_h

#include "Device.h"
#include "MrlMsg.h"

/***********************************************************************
 * NEOPIXEL DEFINE
 */
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

// Arduino Mega Pins
#if defined(ARDUINO_AVR_MEGA2560) || defined(ARDUINO_AVR_ADK)
  #define digitalPinToSendBit(P,V) \
    (((P) >= 22 && (P) <= 29) ? sendBitA(V) : \
        ((((P) >= 10 && (P) <= 13) || ((P) >= 50 && (P) <= 53)) ? sendBitB(V) : \
        (((P) >= 30 && (P) <= 37) ? sendBitC(V) : \
        ((((P) >= 18 && (P) <= 21) || (P) == 38) ? sendBitD(V) : \
        ((((P) >= 0 && (P) <= 3) || (P) == 5) ? sendBitE(V) : \
        (((P) >= 54 && (P) <= 61) ? sendBitF(V) : \
        ((((P) >= 39 && (P) <= 41) || (P) == 4) ? sendBitG(V) : \
        ((((P) >= 6 && (P) <= 9) || (P) == 16 || (P) == 17) ? sendBitH(V) : \
        (((P) == 14 || (P) == 15) ? sendBitJ(V) : \
        (((P) >= 62 && (P) <= 69) ? sendBitK(V) : sendBitL(V)))))))))))

#else
  #define digitalPinToSendBit(P,V) \
    (((P) >= 0 && (P) <= 7) ? sendBitD(V) : (((P) >= 8 && (P) <= 13) ? sendBitB(V) : sendBitC(V)))
#endif

#define NEOPIXEL_ANIMATION_NO_ANIMATION 0
#define NEOPIXEL_ANIMATION_STOP 1
#define NEOPIXEL_ANIMATION_COLOR_WIPE 2
#define NEOPIXEL_ANIMATION_LARSON_SCANNER 3
#define NEOPIXEL_ANIMATION_THEATER_CHASE 4
#define NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW 5
#define NEOPIXEL_ANIMATION_RAINBOW 6
#define NEOPIXEL_ANIMATION_RAINBOW_CYCLE 7
#define NEOPIXEL_ANIMATION_FLASH_RANDOM 8
#define NEOPIXEL_ANIMATION_IRONMAN 9

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
  void clearPixel();
  void setPixel(unsigned char red, unsigned char green, unsigned char blue);
};

class MrlNeopixel:public Device{
  private:
    unsigned int numPixel;  
    Pixel* pixels;
    uint8_t bitmask;
    unsigned long lastShow;
    bool newData;
    int pin;
    unsigned char _baseColorRed;
    unsigned char _baseColorGreen;
    unsigned char _baseColorBlue;
    unsigned int _speed;
    byte _animation;
    unsigned int _pos;
    int _count;
    bool _off;
    int _dir;
    int _step;
    unsigned char _alpha;
  public:
  MrlNeopixel();
  ~MrlNeopixel();
  bool deviceAttach(unsigned char config[], int configSize);
  inline void sendBitB(bool bitVal);
  inline void sendBitC(bool bitVal);
  inline void sendBitD(bool bitVal);
#if defined(ARDUINO_AVR_MEGA2560) || defined(ARDUINO_AVR_ADK)
  inline void sendBitA(bool bitVal);
  inline void sendBitE(bool bitVal);
  inline void sendBitF(bool bitVal);
  inline void sendBitG(bool bitVal);
  inline void sendBitH(bool bitVal);
  inline void sendBitJ(bool bitVal);
  inline void sendBitK(bool bitVal);
  inline void sendBitL(bool bitVal);
#endif
  inline void sendByte(unsigned char byte);
  inline void sendPixel(Pixel p);
  void show();
  void neopixelWriteMatrix(unsigned char* ioCmd);
  void update();
  void setAnimation(unsigned char* config);
  void animationStop();
  void animationColorWipe();
  void animationLarsonScanner();
  void animationTheaterChase();
  void animationWheel(unsigned char WheelPos, Pixel& pixel);
  void animationTheaterChaseRainbow();
  void animationRainbow();
  void animationRainbowCycle();
  void animationFlashRandom();
  void animationIronman();
};



#endif
