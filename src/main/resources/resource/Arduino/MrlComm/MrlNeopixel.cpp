#include <Arduino.h>
#include "Msg.h"
#include "Device.h"
#include "MrlNeopixel.h"

MrlNeopixel::MrlNeopixel(int deviceId) : Device(deviceId, DEVICE_TYPE_NEOPIXEL)
{
}

MrlNeopixel::~MrlNeopixel()
{
  runAnimation = false;
  if (strip)
  {
    strip->clear();
    delete strip;
  }
}

bool MrlNeopixel::attach(byte pin, int count, byte depth)
{
  // FIXME - support "types/depth"
  //  Pixel type flags, add together as needed:
  //   NEO_KHZ800  800 KHz bitstream (most NeoPixel products w/WS2812 LEDs)
  //   NEO_KHZ400  400 KHz (classic 'v1' (not v2) FLORA pixels, WS2811 drivers)
  //   NEO_GRB     Pixels are wired for GRB bitstream (most NeoPixel products)
  //   NEO_RGB     Pixels are wired for RGB bitstream (v1 FLORA pixels, not v2)
  //   NEO_RGBW    Pixels are wired for RGBW bitstream (NeoPixel RGBW products)
  // initialization
  previousWaitMs = millis();
  numPixels = count;
  if(depth == 4)
  {
    strip = new Adafruit_NeoPixel(count, pin, NEO_GRBW + NEO_KHZ800);
  } else{
    strip = new Adafruit_NeoPixel(count, pin, NEO_GRB + NEO_KHZ800);
  }
  strip->begin();
  color = Adafruit_NeoPixel::Color(0, 110, 0, 0);
  wait = 1000;
  return true;
}

bool MrlNeopixel::doneWaiting()
{
  // current time - previous wait timestamp > wait interval
  return millis() - previousWaitMs > wait;
}

// Some functions of our own for creating animated effects -----------------
// Fill strip pixels one after another with a color. Strip is NOT cleared
// first; anything there will be covered pixel by pixel. Pass in color
// (as a single 'packed' 32-bit value, which you can get by calling
// strip->Color(red, green, blue) as shown in the loop() function above),
// and a delay time (in milliseconds) between pixels.
void MrlNeopixel::colorWipe()
{
  strip->setPixelColor(x % numPixels, color);
}

void MrlNeopixel::scanner()
{
  if (y == strip->numPixels() - 1)
  {
    z = 0;
  }

  if (y == 0)
  {
    z = 1;
  }

  if (z)
  {
    y++;
  }
  else
  {
    y--;
  }
  strip->clear(); // Clear all pixels
  strip->setPixelColor(y, color); // Set pixel 'y' to value 'color'
}

// Theater-marquee-style chasing lights. Pass in a color (32-bit value,
// a la strip->Color(r,g,b) as mentioned above), and a delay time (in ms)
// between frames.
void MrlNeopixel::theaterChase()
{
  y = x % 3;
  strip->clear(); //   Set all pixels in RAM to 0 (off)
  // 'c' counts up from 'b' to end of strip in steps of 3...
  for (int c = y; c < strip->numPixels(); c += 3)
  {
    strip->setPixelColor(c, color); // Set pixel 'c' to value 'color'
  }
}

// Displays a rainbow..  no animation i guess?
void MrlNeopixel::rainbow()
{
  for (int i = 0; i < strip->numPixels(); i++)
  {
    int pixelHue = (i * 65536L / strip->numPixels());
    strip->setPixelColor(i, strip->gamma32(strip->ColorHSV(pixelHue)));
  }
}

// Displays a rotating rainbow
void MrlNeopixel::rainbowCycle()
{
  for (int i = 0; i < strip->numPixels(); i++)
  {
    int pixelHue = (x * 256) + (i * 65536L / strip->numPixels());
    strip->setPixelColor(i, strip->gamma32(strip->ColorHSV(pixelHue)));
  }
}

// He was turned to steel in a great magnetic field.
void MrlNeopixel::ironman()
{
  if (x == 0) {
    // initial brightness
    brightness = 127;


  }
  // initialize the color
  strip->fill(color, 0, numPixels);
  int change = random(-16,16);
  // add the incremental random change in the brightness
  brightness = brightness + change;
  // validate limits  min / max brightness
  if (brightness < 32) {
    brightness = 32;
  } else if (brightness > 224) {
    brightness = 224;
  }
  strip->setBrightness(brightness);

  // pluck a random pixel
  int pixelToFlicker = random(0,numPixels);
  strip->setPixelColor(pixelToFlicker, random(0,255), random(0,255), random(0,255));
}

// Rainbow-enhanced theater marquee. Pass delay time (in ms) between frames.
void MrlNeopixel::theaterChaseRainbow()
{
  y = x % 3;
  strip->clear(); //   Set all pixels in RAM to 0 (off)
  // 'c' counts up from 'b' to end of strip in steps of 3...
  for (int c = y; c < strip->numPixels(); c += 3)
  {
    int pixelHue = (x * 256) + (c * 65536L / strip->numPixels());
    strip->setPixelColor(c, strip->gamma32(strip->ColorHSV(pixelHue)));
  }
}

void MrlNeopixel::writeMatrix(byte bufferSize, const byte *buffer)
{
  if (!strip)
  {
    return;
  }

  for (int i = 0; i < bufferSize; i += 5)
  {
    color = Adafruit_NeoPixel::Color(buffer[i + 1], buffer[i + 2], buffer[i + 3], buffer[i + 4]);
    strip->setPixelColor(buffer[i], color);
  }

  strip->show();
}

void MrlNeopixel::setAnimation(byte animation, byte red, byte green, byte blue, byte white, long wait_ms)
{
  animationIndex = animation;
  x = 0;
  y = 0;
  z = 0;
  color = Adafruit_NeoPixel::Color(red, green, blue, white);
  wait = wait_ms;
  if (animation == 0)
  {
    runAnimation = false;
  }
  else
  {
    runAnimation = true;
  }
}

void MrlNeopixel::fill(int firstAddress, int count, byte red, byte green, byte blue, byte white)
{
  if (strip)
  {
    // TRIPLE EXCLAMATION POINTS ARE DEADLY msg->publishError(F("fill!!!!"));
    uint32_t color = ((uint32_t)white << 24) | ((uint32_t)red << 16) | ((uint32_t)green << 8) | (uint32_t)blue;
    strip->fill(color, firstAddress, count);
    strip->show();
  }
}

void MrlNeopixel::setBrightness(byte brightness)
{
  if (strip)
  {
    strip->setBrightness(brightness);
    strip->show();
  }
}

void MrlNeopixel::clear()
{
  runAnimation = false;
  if (strip)
  {
    strip->clear();
    strip->show();
  }
}

void MrlNeopixel::animationFlashRandom() {
  for (int i = 0; i < strip->numPixels(); i++)
  {
    strip->setPixelColor(i, random(0,255), random(0,255), random(0,255));
  }
}

void MrlNeopixel::update()
{
  if (doneWaiting() || x == 0)
  {
    if (runAnimation)
    {
      if (!strip)
      {
        return;
      }
      // Animation only updates after we are done waiting
      switch (animationIndex)
      {
        case NEOPIXEL_ANIMATION_NO_ANIMATION:
        case NEOPIXEL_ANIMATION_STOP:
          runAnimation = false;
          break;
        case NEOPIXEL_ANIMATION_COLOR_WIPE:
          colorWipe();
          break;
        case NEOPIXEL_ANIMATION_LARSON_SCANNER:
          scanner();
          break;
        case NEOPIXEL_ANIMATION_THEATER_CHASE:
          theaterChase();
          break;
        case NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW:
          theaterChaseRainbow();
          break;
        case NEOPIXEL_ANIMATION_RAINBOW:
          rainbow();
          break;
        case NEOPIXEL_ANIMATION_RAINBOW_CYCLE:
          rainbowCycle();
          break;
        case NEOPIXEL_ANIMATION_FLASH_RANDOM:
          animationFlashRandom();
          break;
        case NEOPIXEL_ANIMATION_IRONMAN:
          ironman();
          break;
        default:
          msg->publishError(F("neopixel animation do not exist"));
          break;
      }
      // we've updated our animation.. let's show the result.
      strip->show();
      x++;
      previousWaitMs = millis();
    }
  }
  // current frame index for the animation.
}
