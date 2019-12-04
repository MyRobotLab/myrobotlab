#ifndef MrlAs5048AEncoder_h
#define MrlAs5048AEncoder_h

#include <SPI.h>

/**
 * AS5048A Magnetic Encoder
 * This is a magnetic absolute position 14 bit encoder.
 * It communicates via SPI.
 *
 */
class MrlAs5048AEncoder : public Device {

  private:
  	int csPin; // chip or slave select
	uint16_t ABSposition;
	uint16_t ABSposition_last;
	float deg; // TODO: move this into the update method probably.
	long lastUpdate;
	int updateTimer;
	boolean attached;
	SPISettings settings;
	uint16_t read(SPISettings settings, byte pin, word registerAddress);
	byte spiCalcEvenParity(word value);

  public:
    MrlAs5048AEncoder(int deviceId);
    ~MrlAs5048AEncoder();
    bool attach(byte pin);
    void update();
    // TODO: support this.. this chip is an OTP (one time programming)
    // you can flash/save it once, and that's it. so not implementing it here.
    // void setZeroPoint();
    void publishEncoderData(float deg);
};

#endif
