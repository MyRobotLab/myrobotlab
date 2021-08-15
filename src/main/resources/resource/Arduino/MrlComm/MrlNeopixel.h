#ifndef MrlNeopixel_h
#define MrlNeopixel_h

#include "Adafruit_NeoPixel.h"
#include <Arduino.h>

// TODO: Why two enums to tell the animation to stop?  maybe best to just
// have a stop animation call?
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

class MrlNeopixel : public Device
{
private:
  Adafruit_NeoPixel *strip = NULL;
  byte animationIndex = 0;
  boolean runAnimation = false;

  // for fill and animation colors
  uint32_t color = 0;
  // general pause in ms
  int wait = 100;

  int brightness = 255;

public:
  MrlNeopixel(int deviceId);
  ~MrlNeopixel();

  bool attach(byte pin, int numPixels, byte depth);

  int x;
  int y;
  int z;
  int numPixels;
  long previousWaitMs;

  // utility
  bool doneWaiting();

  // animations
  void colorWipe();
  void theaterChase();
  void rainbow();
  void rainbowCycle();
  void scanner(); 
  void theaterChaseRainbow();
  void animationFlashRandom();
  void ironman();

  // basic methods
  void writeMatrix(byte bufferSize, const byte *buffer);
  void setAnimation(byte animation, byte red, byte green, byte blue, byte white, long wait_ms);
  void fill(int address,  int count,  byte red,  byte green,  byte blue,  byte white);
  void setBrightness(byte brightness);
  void clear();
  
  // the general update
  void update();
};

#endif
