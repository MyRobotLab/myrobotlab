#include <Arduino.h>
#include "Msg.h"
#include "Device.h"
#include "MrlNeopixel2.h"

MrlNeopixel2::MrlNeopixel2(int deviceId) : Device(deviceId, DEVICE_TYPE_NEOPIXEL2)
{
}

MrlNeopixel2::~MrlNeopixel2()
{
	if (strip)
	{
		runAnimation = false;
		strip->clear();
		delete strip;
	}
}

bool MrlNeopixel2::attach(byte pin, int count, byte depth)
{

	// FIXME - support "types/depth"
	//	Pixel type flags, add together as needed:
	//   NEO_KHZ800  800 KHz bitstream (most NeoPixel products w/WS2812 LEDs)
	//   NEO_KHZ400  400 KHz (classic 'v1' (not v2) FLORA pixels, WS2811 drivers)
	//   NEO_GRB     Pixels are wired for GRB bitstream (most NeoPixel products)
	//   NEO_RGB     Pixels are wired for RGB bitstream (v1 FLORA pixels, not v2)
	//   NEO_RGBW    Pixels are wired for RGBW bitstream (NeoPixel RGBW products)

	// initialization
	pixelIndex = 0;
	previousWaitMs = millis();
	numPixels = count;
	strip = new Adafruit_NeoPixel(count, pin, NEO_GRB + NEO_KHZ800);
	strip->begin();
	color = Adafruit_NeoPixel::Color(0, 110, 0, 0);
	wait = 1000;
	return true;
}

bool MrlNeopixel2::doneWaiting()
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
void MrlNeopixel2::colorWipe()
{

	if (!doneWaiting())
	{
		// not done waiting
		// wait_ms - come back when
		// we have..
		return;
	}

	strip->setPixelColor(x % numPixels, color);
	// FIXME - fix show
	strip->show(); //  Update strip to match
	x++;
	previousWaitMs = millis();
}

void MrlNeopixel2::ironman()
{

	if (!doneWaiting())
	{
		// not done waiting
		// wait_ms - come back when
		// we have..
		return;
	}

	if(y == strip->numPixels()-1){
		z = 0;
	} 

	if(y == 0){
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

	strip->clear(); //   Set all pixels in RAM to 0 (off)

	// 'c' counts up from 'b' to end of strip in steps of 3...
	// for (int c = y; c < strip->numPixels(); c += 3)
	// {
		strip->setPixelColor(y, color); // Set pixel 'c' to value 'color'
	//}


	strip->show();
	x++;
	previousWaitMs = millis();
}

void MrlNeopixel2::scanner()
{

	if (!doneWaiting())
	{
		// not done waiting
		// wait_ms - come back when
		// we have..
		return;
	}

	if(y == strip->numPixels()-1){
		z = 0;
	} 

	if(y == 0){
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

	strip->clear(); //   Set all pixels in RAM to 0 (off)

	// 'c' counts up from 'b' to end of strip in steps of 3...
	// for (int c = y; c < strip->numPixels(); c += 3)
	// {
		strip->setPixelColor(y, color); // Set pixel 'c' to value 'color'
	//}


	strip->show();
	x++;
	previousWaitMs = millis();
}

// Theater-marquee-style chasing lights. Pass in a color (32-bit value,
// a la strip->Color(r,g,b) as mentioned above), and a delay time (in ms)
// between frames.
void MrlNeopixel2::theaterChase()
{

	if (!doneWaiting())
	{
		// not done waiting
		// wait_ms - come back when
		// we have..
		return;
	}

	y = x % 3;
	strip->clear(); //   Set all pixels in RAM to 0 (off)

	// 'c' counts up from 'b' to end of strip in steps of 3...
	for (int c = y; c < strip->numPixels(); c += 3)
	{
		strip->setPixelColor(c, color); // Set pixel 'c' to value 'color'
	}
	strip->show();
	x++;
	previousWaitMs = millis();
}

// Rainbow cycle along whole strip. Pass delay time (in ms) between frames.
void MrlNeopixel2::rainbow()
{

	if (!doneWaiting())
	{
		// not done waiting
		// wait_ms - come back when
		// we have..
		return;
	}

	for (int i = 0; i < strip->numPixels(); i++)
	{
		int pixelHue = (x * 256) + (i * 65536L / strip->numPixels());
		strip->setPixelColor(i, strip->gamma32(strip->ColorHSV(pixelHue)));
	}
	// FIXME - fix show
	strip->show(); //  Update strip to match
	x++;
	previousWaitMs = millis();
}

// Rainbow-enhanced theater marquee. Pass delay time (in ms) between frames.
void MrlNeopixel2::theaterChaseRainbow()
{
	if (!doneWaiting())
	{
		// not done waiting
		// wait_ms - come back when
		// we have..
		return;
	}

	y = x % 3;
	strip->clear(); //   Set all pixels in RAM to 0 (off)

	// 'c' counts up from 'b' to end of strip in steps of 3...
	for (int c = y; c < strip->numPixels(); c += 3)
	{
		int pixelHue = (x * 256) + (c * 65536L / strip->numPixels());
		strip->setPixelColor(c, strip->gamma32(strip->ColorHSV(pixelHue)));
	}
	strip->show();
	x++;
	previousWaitMs = millis();
}

void MrlNeopixel2::writeMatrix(byte bufferSize, const byte *buffer)
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

void MrlNeopixel2::setAnimation(byte animation, byte red, byte green, byte blue, byte white, long wait_ms)
{
	animationIndex = animation;
	x = 0;
	pixelIndex = 0;
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

	// colorWipe();
}

void MrlNeopixel2::update()
{
	if (runAnimation)
	{

		switch (animationIndex)
		{
		case NEOPIXEL_ANIMATION_COLOR_WIPE:
			colorWipe();
			break;
		case NEOPIXEL_ANIMATION_RAINBOW:
			rainbow();
			break;
		case NEOPIXEL_ANIMATION_THEATER_CHASE:
			theaterChase();
			break;
		case NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW:
			theaterChaseRainbow();
			break;
		case NEOPIXEL_ANIMATION_LARSON_SCANNER:
			scanner();
			break;
			
		default:
			msg->publishError(F("Neopixel animation do not exist"));
			break;
		}
	}
}
