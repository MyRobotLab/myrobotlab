#ifndef MrlNeopixel_h
#define MrlNeopixel_h

#include "Device.h"
#include "MrlMsg.h"

/***********************************************************************
 * NEOPIXEL DEFINE
 */
#define PIXEL_PORT PORTC
#define PIXEL_DDR DDRC
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
  Pixel(){
    red=0;
    blue=0;
    green=0;
  }
};

class MrlNeopixel:public Device{
  public:
  unsigned int numPixel;
  Pixel* pixels;
  uint8_t bitmask;
  unsigned long lastShow;
  bool newData;
  MrlNeopixel():Device(DEVICE_TYPE_NEOPIXEL){
  }
  ~MrlNeopixel(){
    delete pixels;
  }
  bool deviceAttach(unsigned char config[], int configSize){
    if (configSize != 2){
      MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
      msg.addData(ERROR_DOES_NOT_EXIST);
      msg.addData(String(F("MrlNeopixel invalid attach config size")));
      msg.sendMsg();
      return false;
    }
    int pin=config[0];
    numPixel=config[1];
    pixels = new Pixel[numPixel+1];
    if(BOARD==BOARD_TYPE_ID_UNKNOWN) {
      MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
      msg.addData(ERROR_DOES_NOT_EXIST);
      msg.addData(String(F("Board not supported")));
      msg.sendMsg();
      //mrlComm.publishError(ERROR_DOES_NOT_EXIST,F("Board not supported"));
      return false;
    }
    if(pin<30 || pin>37) {
      MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
      msg.addData(ERROR_DOES_NOT_EXIST);
      msg.addData(String(F("Pin not supported")));
      msg.sendMsg();
      return false;
    }
    else{
      state=1;
      bitmask=digitalPinToBitMask(pin);
    }
    PIXEL_DDR |= bitmask;
    lastShow=0;
    Pixel pixel=Pixel();
    for (unsigned int i=1; i<=numPixel; i++) {
      pixels[i] = pixel;
    }
    newData=true;
    attachDevice();
    return true;
  }
  inline void sendBit(bool bitVal){
    uint8_t bit=bitmask;
    if (bitVal) {        // 0 bit
      PIXEL_PORT |= bit;
      asm volatile (
        ".rept %[onCycles] \n\t"                                // Execute NOPs to delay exactly the specified number of cycles
        "nop \n\t"
        ".endr \n\t"
        ::
        [onCycles]  "I" (NS_TO_CYCLES(T1H) - 2)    // 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
        );
      PIXEL_PORT &= ~bit;
      asm volatile (
        ".rept %[offCycles] \n\t"                               // Execute NOPs to delay exactly the specified number of cycles
        "nop \n\t"
        ".endr \n\t"
        ::
        [offCycles]   "I" (NS_TO_CYCLES(T1L) - 2)     // Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
      );
    } else {          // 1 bit
      // **************************************************************************
      // This line is really the only tight goldilocks timing in the whole program!
      // **************************************************************************
      cli(); //desactivate interrupts
      PIXEL_PORT |= bit ;
      asm volatile (
        ".rept %[onCycles] \n\t"        // Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
        "nop \n\t"                                              // Execute NOPs to delay exactly the specified number of cycles
        ".endr \n\t"
        ::
        [onCycles]  "I" (NS_TO_CYCLES(T0H) - 2)
        );
      PIXEL_PORT &= ~bit;
      asm volatile (
        ".rept %[offCycles] \n\t"                               // Execute NOPs to delay exactly the specified number of cycles
        "nop \n\t"
        ".endr \n\t"
        ::
        [offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
      );
      sei(); //activate interrupts
    }
      // Note that the inter-bit gap can be as long as you want as long as it doesn't exceed the 5us reset timeout (which is A long time)
      // Here I have been generous and not tried to squeeze the gap tight but instead erred on the side of lots of extra time.
      // This has thenice side effect of avoid glitches on very long strings becuase 
  }
    
  inline void sendByte(unsigned char byte) {
    for(unsigned char bit = 0 ; bit < 8 ; bit++ ) {
      sendBit( bitRead( byte , 7 ) );                // Neopixel wants bit in highest-to-lowest order
                                                     // so send highest bit (bit #7 in an 8-bit byte since they start at 0)
      byte <<= 1;                                    // and then shift left so bit 6 moves into 7, 5 moves into 6, etc
    }           
  }
  inline void sendPixel(Pixel p) {  
    sendByte(p.green);          // Neopixel wants colors in green then red then blue order
    sendByte(p.red);
    sendByte(p.blue);
  }
  void show(){
    if (!state) return;
    //be sure we wait at least 6us before sending new data
    if ((lastShow+(RES/1000UL))>micros()) return;
    for(unsigned int p=1; p<=numPixel;p++){
      sendPixel(pixels[p]);
    }
    lastShow=micros();
    newData=false;

  }
  void neopixelWriteMatrix(unsigned char* ioCmd) {
    for (int i=3; i<ioCmd[2]+3;i+=4){
      pixels[ioCmd[i]].red=ioCmd[i+1];
      pixels[ioCmd[i]].green=ioCmd[i+2];
      pixels[ioCmd[i]].blue=ioCmd[i+3];
    }
    newData=true;
  }
  void update(unsigned long lastMicros){
    if((lastShow+33000)>micros() || !newData) return; //update 30 times/sec if there is new data to show
    show();
  }
};



#endif
