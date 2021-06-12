#ifndef MrlNeopixel2_h
#define MrlNeopixel2_h

#include "Adafruit_NeoPixel.h"
#include <Arduino.h>

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

class Msg;

class MrlNeopixel2 : public Device
{
private:
  Adafruit_NeoPixel *strip = NULL;
  byte animationIndex = 0;
  boolean runAnimation = false;

  // for fill and animation colors
  uint32_t color = 0;
  // general pause in ms
  int wait = 100;

public:
  MrlNeopixel2(int deviceId);
  ~MrlNeopixel2();

  bool attach(byte pin, int numPixels, byte depth);

  int x;
  int y;
  int z;
  int numPixels;
  int pixelIndex;
  long previousWaitMs;
  bool doneWaiting();

  // animations
  void colorWipe();
  void theaterChase();
  void rainbow(); 
  void scanner(); 
  void theaterChaseRainbow();

  void writeMatrix(byte bufferSize, const byte *buffer);
  void setAnimation(byte animation, byte red, byte green, byte blue, byte white, long wait_ms);

  void update();

  /*
  AdaFruit Library methods 

  void begin(void);
  void show(void);
  void setPin(uint16_t p);
  void setPixelColor(uint16_t n, uint8_t r, uint8_t g, uint8_t b);
  void setPixelColor(uint16_t n, uint8_t r, uint8_t g, uint8_t b,
                     uint8_t w);

  void setPixelColor(uint16_t n, uint32_t c);
  void fill(uint32_t c = 0, uint16_t first = 0, uint16_t count = 0);
  void setBrightness(uint8_t);
  void clear(void);
  void updateLength(uint16_t n);
  void updateType(neoPixelType t);
  */
};

#endif
