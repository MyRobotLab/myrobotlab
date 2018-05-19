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

}

MrlAmt203Encoder::~MrlAmt203Encoder() {
  // destructor
}

// helper function to transmit a message to the encoder
uint8_t MrlAmt203Encoder::SPI_T(uint8_t msg) {
  //Repetive SPI transmit sequence
   uint8_t msg_temp = 0;  //vairable to hold recieved data
   digitalWrite(csPin,LOW);     //select spi device
   msg_temp = SPI.transfer(msg);    //send and recieve
   digitalWrite(csPin,HIGH);    //deselect spi device
   return(msg_temp);      //return recieved byte
}


bool MrlAmt203Encoder::attach(byte pin){
	// TODO: do i need to cast this to int from byte?!
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
	msg->publishDebug("Update encoder");

  uint8_t recieved = 0xA5;    //just a temp vairable
  SPI.begin();    //start transmition
  SPI_T(0x10);   //issue read command

  // TODO: maybe move this to the attach
  while (recieved != 0x10) {
   //loop while encoder is not ready to send
    recieved = SPI_T(0x00);    //cleck again if encoder is still working
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
    float deg = ABSposition * 0.087890625;    // 360/4096  degrees to 12 bit resolution
    this->publishEncoderPosition(deg);
  }
  // TODO: remove this , if we don't have some delay between this update and the next, the encoder doesn't seem happy.
  delay(10);
}

void MrlAmt203Encoder::publishEncoderPosition(float deg) {
  // TODO: publish an mrl message with the current encoder position.
  msg->publishEncoderPosition(id, deg);
}
