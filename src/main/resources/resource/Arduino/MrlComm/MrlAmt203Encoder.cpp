#include <SPI.h>
#include "Msg.h"
#include "Device.h"
#include "MrlAmt203Encoder.h"

MrlAmt203Encoder::MrlAmt203Encoder(int deviceId) : Device(deviceId, DEVICE_TYPE_ENCODER) {
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

MrlAmt203Encoder::~MrlAmt203Encoder() {
  // destructor
}

// helper function to transmit a message to the encoder
uint8_t MrlAmt203Encoder::SPI_T(uint8_t data) {
  //Repetive SPI transmit sequence
   uint8_t msg_temp = 0;  //vairable to hold recieved data
   digitalWrite(csPin,LOW);     //select spi device
   msg_temp = SPI.transfer(data);    //send and recieve
   digitalWrite(csPin,HIGH);    //deselect spi device
   return(msg_temp);      //return recieved byte
}

bool MrlAmt203Encoder::attach(byte pin){
	// TODO: does it hurt to set up the bus each time a new encoder is added?
  attached = true;
  this->csPin = pin;
  // set up the spi bus
  pinMode(pin,OUTPUT);//Slave Select
  digitalWrite(pin,HIGH);
  SPI.begin();
  SPI.setBitOrder(MSBFIRST);
  SPI.setDataMode(SPI_MODE0);
  SPI.setClockDivider(SPI_CLOCK_DIV32);
  SPI.end();
  ABSposition = 0;    //reset position vairable
  // TODO: implement a detach feature.
}

void MrlAmt203Encoder::update() {
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
  uint8_t recieved = 0xA5;    //just a temp vairable
  SPI.begin();    //start transmition
  SPI_T(0x10);   //issue read command
  // TODO: maybe move this to the attach
  while (recieved != 0x10) {
   // TODO: this could hang MrlComm if we don't get a response from an encoder.. and that'd be bad! mkay!
   //loop while encoder is not ready to send
    recieved = SPI_T(0x00);    //check again if encoder is still working
  }
  temp[0] = SPI_T(0x00);    //Recieve MSB
  temp[1] = SPI_T(0x00);    //Recieve LSB
  SPI.end();    //end transmition
  // assemble the 12 bit value
  temp[0] &=~ 0xF0;    //mask out the first 4 bits
  ABSposition = temp[0] << 8;    //shift MSB to correct ABSposition in ABSposition message
  ABSposition += temp[1];    // add LSB to ABSposition message to complete message
  if (ABSposition != ABSposition_last) {
   //if nothing has changed dont wast time sending position
    ABSposition_last = ABSposition;    //set last position to current position
    // deg = ABSposition;
    //float deg = ABSposition * 0.087890625;    // 360/4096  degrees to 12 bit resolution
    this->publishEncoderData(ABSposition);
  }
}

void MrlAmt203Encoder::setZeroPoint() {
  // send the command 0x70 to calibrate the encoder.
  //
  //	To set the zero point this sequence should be followed:
  //	1. Send set_zero_point command
  //	2. Send nop_a5 command while response is not 0x80
  //	3. A response of 0x80 means that the zero set was successful and the new position offset is stored in EEPROM.
  //	4. The encoder must be power cycled. If the encoder is not power cycled, the position values will not be calculated off the latest
  //	 zero position. When the encoder is powered on next the new offset will be used for the position calculation.
  uint8_t recieved = 0xA5; // a temp variable to read the response
  // This is step 1, send the set zero point command
  SPI.begin();    //start transmition
  SPI_T(0x70);    // send the set zero point calibration command
  // now wait for it to be ready.
  do {
	  recieved = SPI_T(0x00); // poll until we're ready/done
  } while (recieved != 0xA5);
  SPI.end();    // stop the spi bus for now.
}

void MrlAmt203Encoder::publishEncoderData(float deg) {
  // publish an mrl message with the current encoder position.
  msg->publishEncoderData(id, deg);
}
