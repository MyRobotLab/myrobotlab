#include "MrlNeopixel.h"

Pixel::Pixel() {
	clearPixel();
}

void Pixel::clearPixel() {
	red = 0;
	blue = 0;
	green = 0;
}

void Pixel::setPixel(unsigned char red, unsigned char green, unsigned char blue){
	this->red = red;
	this->green = green;
	this->blue = blue;
}

MrlNeopixel::MrlNeopixel():Device(DEVICE_TYPE_NEOPIXEL) {
	_baseColorRed = 0;
	_baseColorGreen = 0;
	_baseColorBlue = 0;
	_animation = 0;
}

MrlNeopixel::~MrlNeopixel() {
	animationStop();
	show();
	delete pixels;
}

bool MrlNeopixel::deviceAttach(unsigned char config[], int configSize) {
	if (configSize != 2) {
		MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
		msg.addData(ERROR_DOES_NOT_EXIST);
		msg.addData(String(F("MrlNeopixel invalid attach config size")));
		msg.sendMsg();
		return false;
	}
	pin = config[0];
	numPixel = config[1];
	pixels = new Pixel[numPixel + 1];
	if (BOARD == BOARD_TYPE_ID_UNKNOWN) {
		MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
		msg.addData(ERROR_DOES_NOT_EXIST);
		msg.addData(String(F("Board not supported")));
		msg.sendMsg();
		return false;
	}
	state = 1;
	bitmask = digitalPinToBitMask(pin);
	pinMode(pin, OUTPUT);
	lastShow = 0;
	Pixel pixel = Pixel();
	for (unsigned int i = 1; i <= numPixel; i++) {
		pixels[i] = pixel;
	}
	newData = true;
	attachDevice();
	return true;
}

inline void MrlNeopixel::sendBitB(bool bitVal) {
	uint8_t bit = bitmask;
	if (bitVal) {        // 0 bit
		PORTB |= bit;
		asm volatile (
				".rept %[onCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTB &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();
		//desactivate interrupts
		PORTB |= bit;
		asm volatile (
				".rept %[onCycles] \n\t" // Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTB &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();
		//activate interrupts
	}
}

inline void MrlNeopixel::sendBitC(bool bitVal) {
	uint8_t bit = bitmask;
	if (bitVal) {        // 0 bit
		PORTC |= bit;
		asm volatile (
				".rept %[onCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTC &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();
		//desactivate interrupts
		PORTC |= bit;
		asm volatile (
				".rept %[onCycles] \n\t" // Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTC &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();
		//activate interrupts
	}
	// Note that the inter-bit gap can be as long as you want as long as it doesn't exceed the 5us reset timeout (which is A long time)
	// Here I have been generous and not tried to squeeze the gap tight but instead erred on the side of lots of extra time.
	// This has thenice side effect of avoid glitches on very long strings becuase
}
#if defined(ARDUINO_AVR_MEGA2560) || defined(ARDUINO_AVR_ADK)

inline void MrlNeopixel::sendBitL(bool bitVal) {
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTL |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTL &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTL |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTL &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
}

inline void MrlNeopixel::sendBitK(bool bitVal) {
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTK |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTK &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTK |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTK &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
}

inline void MrlNeopixel::sendBitJ(bool bitVal) {
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTJ |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTJ &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTJ |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTJ &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
}

inline void MrlNeopixel::sendBitH(bool bitVal) {
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTH |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTH &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTH |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTH &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
}

inline void MrlNeopixel::sendBitG(bool bitVal) {
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTG |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTG &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTG |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTG &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
}

inline void MrlNeopixel::sendBitF(bool bitVal) {
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTF |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTF &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTF |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTF &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
}

inline void MrlNeopixel::sendBitE(bool bitVal) {
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTE |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTE &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTE |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTE &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
}

inline void MrlNeopixel::sendBitA(bool bitVal) {
	//Serial.println(bitmask);
	uint8_t bit=bitmask;
	if (bitVal) {        // 0 bit
		PORTA |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTA &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();//desactivate interrupts
		PORTA |= bit;
		asm volatile (
				".rept %[onCycles] \n\t"// Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTA &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t"// Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();//activate interrupts
	}
	// Note that the inter-bit gap can be as long as you want as long as it doesn't exceed the 5us reset timeout (which is A long time)
	// Here I have been generous and not tried to squeeze the gap tight but instead erred on the side of lots of extra time.
	// This has thenice side effect of avoid glitches on very long strings becuase
}

#endif

inline void MrlNeopixel::sendBitD(bool bitVal) {
	uint8_t bit = bitmask;
	if (bitVal) {        // 0 bit
		PORTD |= bit;
		asm volatile (
				".rept %[onCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T1H) - 2)// 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
		);
		PORTD &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T1L) - 2)// Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
		);
	} else {          // 1 bit
		// **************************************************************************
		// This line is really the only tight goldilocks timing in the whole program!
		// **************************************************************************
		cli();
		//desactivate interrupts
		PORTD |= bit;
		asm volatile (
				".rept %[onCycles] \n\t" // Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
				"nop \n\t"// Execute NOPs to delay exactly the specified number of cycles
				".endr \n\t"
				::
				[onCycles] "I" (NS_TO_CYCLES(T0H) - 2)
		);
		PORTD &= ~bit;
		asm volatile (
				".rept %[offCycles] \n\t" // Execute NOPs to delay exactly the specified number of cycles
				"nop \n\t"
				".endr \n\t"
				::
				[offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
		);
		sei();
		//activate interrupts
	}

}

inline void MrlNeopixel::sendByte(unsigned char byte) {
	for (unsigned char bit = 0; bit < 8; bit++) {
		bool val = bitRead(byte, 7);
		digitalPinToSendBit(pin, val);
		// sendBit( bitRead( byte , 7 ) );                // Neopixel wants bit in highest-to-lowest order
		// so send highest bit (bit #7 in an 8-bit byte since they start at 0)
		byte <<= 1; // and then shift left so bit 6 moves into 7, 5 moves into 6, etc
	}
}

inline void MrlNeopixel::sendPixel(Pixel p) {
	sendByte(p.green); // Neopixel wants colors in green then red then blue order
	sendByte(p.red);
	sendByte(p.blue);
}

void MrlNeopixel::show() {
	if (!state)
		return;
	//be sure we wait at least 6ms before sending new data
	if ((lastShow + (RES / 1000UL)) > millis())
//  if ((lastShow + RES) > millis())
		return;
	for (unsigned int p = 1; p <= numPixel; p++) {
		sendPixel(pixels[p]);
	}
	lastShow = millis();
	newData = false;

}

void MrlNeopixel::neopixelWriteMatrix(unsigned char* ioCmd) {
	for (int i = 3; i < ioCmd[2] + 3; i += 4) {
		pixels[ioCmd[i]].red = ioCmd[i + 1];
		pixels[ioCmd[i]].green = ioCmd[i + 2];
		pixels[ioCmd[i]].blue = ioCmd[i + 3];
	}
	newData = true;
}

void MrlNeopixel::update() {
	if ((lastShow + 33) > millis()){
		return; //update 30 times/sec if there is new data to show
	}
	switch (_animation) {
  case NEOPIXEL_ANIMATION_NO_ANIMATION:
    break;
	case NEOPIXEL_ANIMATION_STOP:
		animationStop();
		break;
	case NEOPIXEL_ANIMATION_COLOR_WIPE:
		animationColorWipe();
		break;
  case NEOPIXEL_ANIMATION_LARSON_SCANNER:
    animationLarsonScanner();
    break;
  case NEOPIXEL_ANIMATION_THEATER_CHASE:
    animationTheaterChase();
    break;
  case NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW:
    animationTheaterChaseRainbow();
    break;
  case NEOPIXEL_ANIMATION_RAINBOW:
    animationRainbow();
    break;
  case NEOPIXEL_ANIMATION_RAINBOW_CYCLE:
    animationRainbowCycle();
    break;
  case NEOPIXEL_ANIMATION_FLASH_RANDOM:
    animationFlashRandom();
    break;
  case NEOPIXEL_ANIMATION_IRONMAN:
    animationIronman();
    break;
	default:
		MrlMsg::publishError(ERROR_DOES_NOT_EXIST,
				F("Neopixel animation do not exist"));
		break;
	}
	if(newData){
	  show();
	}
}

void MrlNeopixel::setAnimation(unsigned char* config){
	unsigned char size = config[0];
	if (size != 6){
		MrlMsg::publishError(ERROR_DOES_NOT_EXIST, F("Wrong config size for Neopixel setAnimation"));
		return;
	}
	_animation = config[1];
	_baseColorRed = config[2];
	_baseColorGreen = config[3];
	_baseColorBlue = config[4];
	_speed = (config[5] << 8) + config[6];
  _pos = 1;
  _count = 0;
  _off = false;
  _dir = 1;
  _step = 1;
  _alpha = 50;
  newData=true;
}

void MrlNeopixel::animationStop() {
	for (unsigned int i = 1; i <= numPixel; i++) {
		pixels[i].clearPixel();
	}
  _animation = NEOPIXEL_ANIMATION_NO_ANIMATION;
	newData = true;
}

void MrlNeopixel::animationColorWipe() {
  if(!((_count++)%_speed)) {
  	if(_off) {
      pixels[_pos++].setPixel(0, 0, 0);
  	}
    else{
  		pixels[_pos++].setPixel(_baseColorRed, _baseColorGreen, _baseColorBlue);
    }
    if(_pos > numPixel) {
      _pos = 1;
      _off = !_off;
    }
	}
  else lastShow = millis();
  newData = true;
}

void MrlNeopixel::animationLarsonScanner() {
  if(!((_count++)%_speed)) {
    for(unsigned int i = 1; i <= numPixel; i++){
      pixels[i].clearPixel();
    }
    unsigned int pos = _pos;
    for(int i = -2; i <= 2; i++){
      pos = _pos + i;
      if (pos < 1)
        pos += numPixel;
      if (pos > numPixel)
        pos -= numPixel;
      int j = (abs(i) * 10)+1;
      pixels[pos].setPixel(_baseColorRed / j, _baseColorGreen /j, _baseColorBlue / j);
    }
    _pos += _dir;
    if (_pos < 1) {
      pos = 2;
      _dir = -_dir;
   }
    else if(_pos > numPixel) {
      _pos = numPixel - 1;
      _dir = -_dir;
    }
  }
  else lastShow = millis();
  newData = true;  
}

void MrlNeopixel::animationTheaterChase() {
  if(!((_count++)%_speed)) {
    for (unsigned int i = 0; i <= numPixel; i+=3){
      if(i + _pos <= numPixel){
        pixels[i + _pos].clearPixel();
      }
    }
    _pos++;
    if(_pos >= 4) _pos = 1;
    for (unsigned int i = 0; i <= numPixel; i+=3){
      if(i + _pos <= numPixel){
        pixels[i + _pos].setPixel(_baseColorRed, _baseColorGreen, _baseColorBlue);
      }
    }
  }
  else lastShow = millis();
  newData = true;  
}

void MrlNeopixel::animationWheel(unsigned char WheelPos, Pixel& pixel) {
  WheelPos = 255 - WheelPos;
  if(WheelPos < 85) {
    pixel.setPixel(255 - WheelPos * 3, 0 , WheelPos * 3);
  }
  else if(WheelPos < 170) {
    WheelPos -= 85;
    pixel.setPixel(0, WheelPos * 3, 255 - WheelPos * 3);
  }
  else {
    WheelPos -= 170;
    pixel.setPixel(WheelPos * 3, 255 - WheelPos * 3, 0);
  }
}

void MrlNeopixel::animationTheaterChaseRainbow() {
  if(!((_count++)%_speed)) {
    for (unsigned int i = 0; i <= numPixel; i+=3){
      if(i + _pos <= numPixel){
        pixels[i + _pos].clearPixel();
      }
    }
    _pos++;
    if(_pos >= 4) _pos = 1;
    for (unsigned int i = 0; i <= numPixel; i+=3){
      if(i + _pos <= numPixel){
        animationWheel((_baseColorRed + i), pixels[i + _pos]);
      }
    }
    _baseColorRed++;
  }
  else lastShow = millis();
  newData = true;  
}

void MrlNeopixel::animationRainbow() {
  if(!((_count++)%_speed)) {
    for (unsigned int i = 0; i <= numPixel; i++){
      animationWheel((_baseColorRed + i), pixels[i]);
    }
    _baseColorRed++;
  }
  else lastShow = millis();
  newData = true;  
}

void MrlNeopixel::animationRainbowCycle() {
  if(!((_count++)%_speed)) {
    for (unsigned int i = 0; i <= numPixel; i++){
      animationWheel((i * 256 / numPixel) + _baseColorRed, pixels[i]);
    }
    _baseColorRed++;
  }
  else lastShow = millis();
  newData = true;  
}

void MrlNeopixel::animationFlashRandom() {
  if(!((_count++)%_speed)) {
    if(_step == 1){
      _pos = random(numPixel)+1;
    }
    if (_step < 6){
      int r = (_baseColorRed * _step) / 5;
      int g = (_baseColorGreen * _step) / 5;
      int b = (_baseColorBlue * _step) / 5;
      pixels[_pos].setPixel(r, g, b);
    }
    else{
      int r = (_baseColorRed * (11-_step)) / 5;
      int g = (_baseColorGreen * (11-_step)) / 5;
      int b = (_baseColorBlue * (11-_step)) / 5;
      pixels[_pos].setPixel(r, g, b);
    }
    _step++;
    if(_step > 11) _step = 1;
  }
  else lastShow = millis();
  newData = true;  
}

void MrlNeopixel::animationIronman() {
  if(!((_count++)%_speed)) {
    int flip = random(32);
    if (flip > 22) _dir = -_dir;
    _alpha += 5 * _dir;
    if (_alpha < 5) {
      _alpha = 5;
      _dir = 1;
    }
    if (_alpha > 100) {
      _alpha = 100;
      _dir = -1;
    }
    for (unsigned int i = 1; i <= numPixel; i++){
      pixels[i].setPixel((_baseColorRed * _alpha) / 100, (_baseColorGreen * _alpha) / 100, (_baseColorBlue * _alpha) / 100);  
    }
  }
  else lastShow = millis();
  newData = true;  
}

