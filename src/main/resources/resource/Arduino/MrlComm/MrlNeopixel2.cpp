#include <Arduino.h>
#include "Msg.h"
#include "Device.h"
#include "MrlNeopixel2.h"

MrlNeopixel2::MrlNeopixel2(int deviceId) : Device(deviceId, DEVICE_TYPE_NEOPIXEL)
{
}

MrlNeopixel2::~MrlNeopixel2()
{
	// stop animations ?
	// clear ?
	// delete strip
	delete strip;
}
// FIXME DELETE SHOULD BE IN DETACH !!

bool MrlNeopixel2::attach(byte pin, int numPixels, byte depth)
{

	// FIXME - support "types/depth"
	//	Pixel type flags, add together as needed:
	//   NEO_KHZ800  800 KHz bitstream (most NeoPixel products w/WS2812 LEDs)
	//   NEO_KHZ400  400 KHz (classic 'v1' (not v2) FLORA pixels, WS2811 drivers)
	//   NEO_GRB     Pixels are wired for GRB bitstream (most NeoPixel products)
	//   NEO_RGB     Pixels are wired for RGB bitstream (v1 FLORA pixels, not v2)
	//   NEO_RGBW    Pixels are wired for RGBW bitstream (NeoPixel RGBW products)
	strip = new Adafruit_NeoPixel(numPixels, pin, NEO_GRB + NEO_KHZ800);
	strip->begin();
	
	return true;
}

// Some functions of our own for creating animated effects -----------------

// Fill strip pixels one after another with a color. Strip is NOT cleared
// first; anything there will be covered pixel by pixel. Pass in color
// (as a single 'packed' 32-bit value, which you can get by calling
// strip->Color(red, green, blue) as shown in the loop() function above),
// and a delay time (in milliseconds) between pixels.
void MrlNeopixel2::colorWipe()
{
	for (int i = 0; i < strip->numPixels(); i++)
	{									// For each pixel in strip...
		strip->setPixelColor(i, color); //  Set pixel's color (in RAM)
		strip->show();					//  Update strip to match
		delay(wait);					//  Pause for a moment
	}
}

// Theater-marquee-style chasing lights. Pass in a color (32-bit value,
// a la strip->Color(r,g,b) as mentioned above), and a delay time (in ms)
// between frames.
void MrlNeopixel2::theaterChase()
{
	for (int a = 0; a < 10; a++)
	{ // Repeat 10 times...
		for (int b = 0; b < 3; b++)
		{					//  'b' counts from 0 to 2...
			strip->clear(); //   Set all pixels in RAM to 0 (off)
			// 'c' counts up from 'b' to end of strip in steps of 3...
			for (int c = b; c < strip->numPixels(); c += 3)
			{
				strip->setPixelColor(c, color); // Set pixel 'c' to value 'color'
			}
			strip->show(); // Update strip with new contents
			delay(wait);   // Pause for a moment
		}
	}
}

// Rainbow cycle along whole strip. Pass delay time (in ms) between frames.
void MrlNeopixel2::rainbow()
{
	// Hue of first pixel runs 5 complete loops through the color wheel.
	// Color wheel has a range of 65536 but it's OK if we roll over, so
	// just count from 0 to 5*65536. Adding 256 to firstPixelHue each time
	// means we'll make 5*65536/256 = 1280 passes through this outer loop:
	for (long firstPixelHue = 0; firstPixelHue < 5 * 65536; firstPixelHue += 256)
	{
		for (int i = 0; i < strip->numPixels(); i++)
		{ // For each pixel in strip->..
			// Offset pixel hue by an amount to make one full revolution of the
			// color wheel (range of 65536) along the length of the strip
			// (strip->numPixels() steps):
			int pixelHue = firstPixelHue + (i * 65536L / strip->numPixels());
			// strip->ColorHSV() can take 1 or 3 arguments: a hue (0 to 65535) or
			// optionally add saturation and value (brightness) (each 0 to 255).
			// Here we're using just the single-argument hue variant. The result
			// is passed through strip->gamma32() to provide 'truer' colors
			// before assigning to each pixel:
			strip->setPixelColor(i, strip->gamma32(strip->ColorHSV(pixelHue)));
		}
		strip->show(); // Update strip with new contents
		delay(wait);   // Pause for a moment
	}
}

// Rainbow-enhanced theater marquee. Pass delay time (in ms) between frames.
void MrlNeopixel2::theaterChaseRainbow()
{
	int firstPixelHue = 0; // First pixel starts at red (hue 0)
	for (int a = 0; a < 30; a++)
	{ // Repeat 30 times...
		for (int b = 0; b < 3; b++)
		{					//  'b' counts from 0 to 2...
			strip->clear(); //   Set all pixels in RAM to 0 (off)
			// 'c' counts up from 'b' to end of strip in increments of 3...
			for (int c = b; c < strip->numPixels(); c += 3)
			{
				// hue of pixel 'c' is offset by an amount to make one full
				// revolution of the color wheel (range 65536) along the length
				// of the strip (strip->numPixels() steps):
				int hue = firstPixelHue + c * 65536L / strip->numPixels();
				uint32_t color = strip->gamma32(strip->ColorHSV(hue)); // hue -> RGB
				strip->setPixelColor(c, color);						   // Set pixel 'c' to value 'color'
			}
			strip->show();				 // Update strip with new contents
			delay(wait);				 // Pause for a moment
			firstPixelHue += 65536 / 90; // One cycle of color wheel over 90 frames
		}
	}
}

void MrlNeopixel2::writeMatrix(byte bufferSize, const byte *buffer)
{
}

void MrlNeopixel2::setAnimation(byte animation, byte red, byte green, byte blue, int interval_ms)
{
	animationIndex = animation;
	color = Adafruit_NeoPixel::Color(red, green, blue, 0); // white ? were'd it go?
	wait = interval_ms;
	if (animation == 0)
	{
		runAnimation = false;
	}
	else
	{
		runAnimation = true;
	}
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
		default:
			msg->publishError(F("Neopixel animation do not exist"));
			break;
		}
	}
}
