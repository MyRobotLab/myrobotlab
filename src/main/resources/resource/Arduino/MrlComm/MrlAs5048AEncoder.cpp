#include <SPI.h>
#include "Msg.h"
#include "Device.h"
#include "MrlAs5048AEncoder.h"

const int AS5048A_ANGLE = 0x3FFF;
const int AS5048A_MAGNITUDE = 0x3FFE;

MrlAs5048AEncoder::MrlAs5048AEncoder(int deviceId) : Device(deviceId, DEVICE_TYPE_ENCODER) {
	// constructor!
	csPin = 0;
	ABSposition = 0;
	ABSposition_last = 0;
	deg = 0.0;
	lastUpdate = millis();
	// 10 ms delay between reads of a given encoder
	updateTimer = 10;
	attached = false;
}

MrlAs5048AEncoder::~MrlAs5048AEncoder() {
  // destructor
}

bool MrlAs5048AEncoder::attach(byte pin){
	// TODO: does it hurt to set up the bus each time a new encoder is added?
  attached = true;
  this->csPin = pin;
  // set up the spi bus
  pinMode(pin,OUTPUT);//Slave Select
  digitalWrite(pin,HIGH);
  SPI.begin();
  SPI.setBitOrder(MSBFIRST);
  SPI.setDataMode(SPI_MODE1);
  SPI.setClockDivider(SPI_CLOCK_DIV32);
  SPI.end();
  ABSposition = 0;    //reset position vairable

  settings = SPISettings(1000000, MSBFIRST, SPI_MODE1);

  // TODO: implement a detach feature.
}

void MrlAs5048AEncoder::update() {
  if (!attached) {
    return;
  }
  // msg->publishDebug("Update encoder");
  // Only update if at least 10 ms have passed since the last update.
  // TODO: consider making this lower and see where it breaks.
  long now = millis();
  if (now - lastUpdate > updateTimer) {
    lastUpdate = now;
  } else {
    return;
  }

  // get the new reading here!
  SPI.begin();
  ABSposition = read(settings, csPin, AS5048A_ANGLE);
  SPI.end();

  if (ABSposition != ABSposition_last) {
   //if nothing has changed dont wast time sending position
    ABSposition_last = ABSposition;    //set last position to current position
    // deg = ABSposition;
    // float deg = ABSposition * 0.02197265625;    // 360/16384  degrees to 12 bit resolution
    this->publishEncoderData(ABSposition);
  }
}

uint16_t MrlAs5048AEncoder::read(SPISettings settings, byte pin, word registerAddress) {

	// TODO: expose and pay attention to this!
  boolean errorFlag = false;

  word command = 0b0100000000000000; // PAR=0 R/W=R
  command = command | registerAddress;

  //Add a parity bit on the the MSB
  command |= ((word)spiCalcEvenParity(command)<<15);

  //Split the command into two bytes
  byte right_byte = command & 0xFF;
  byte left_byte = ( command >> 8 ) & 0xFF;

  //SPI - begin transaction
  SPI.beginTransaction(settings);

  //Send the command
  digitalWrite(pin, LOW);
  SPI.transfer(left_byte);
  SPI.transfer(right_byte);
  digitalWrite(pin,HIGH);

  //Now read the response
  digitalWrite(pin, LOW);
  left_byte = SPI.transfer(0x00);
  right_byte = SPI.transfer(0x00);
  digitalWrite(pin, HIGH);

  //SPI - end transaction
  SPI.endTransaction();

  //Check if the error bit is set
  if (left_byte & 0x40) {
    errorFlag = true;
  } else {
    errorFlag = false;
  }

  //Return the data, stripping the parity and error bits
  return (( ( left_byte & 0xFF ) << 8 ) | ( right_byte & 0xFF )) & ~0xC000;
}


byte MrlAs5048AEncoder::spiCalcEvenParity(word value) {
  byte cnt = 0;
  byte i;

  for (i = 0; i < 16; i++)
  {
    if (value & 0x1)
    {
      cnt++;
    }
    value >>= 1;
  }
  return cnt & 0x1;
}


void MrlAs5048AEncoder::publishEncoderData(float deg) {
  // publish an mrl message with the current encoder position.
  msg->publishEncoderData(id, deg);
}
