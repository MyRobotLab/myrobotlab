#ifndef MrlAs5048AEncoder_h
#define MrlAs5048AEncoder_h


/**
 * AMT203 Encoder by CUI.
 * This is a capactive absolute position 12 bit encoder.
 * It communicates via SPI.
 *
 */
class MrlAs5048AEncoder : public Device {

  private:
  	int csPin; // chip or slave select
	uint16_t ABSposition;
	uint16_t ABSposition_last;
	uint8_t temp[2];    //to hold the incoming reading
	float deg; // TODO: move this into the update method probably.
	long lastUpdate;
	int updateTimer;
	boolean attached;
    uint8_t SPI_T(uint8_t msg);

  public:
    MrlAs5048AEncoder(int deviceId);
    ~MrlAs5048AEncoder();
    bool attach(byte pin);
    void update();
    // TODO: support this.. this chip is an OTP (one time programming)
    // you can flash/save it once, and that's it. so not implementing it here.
    // void setZeroPoint();
    void publishEncoderPosition(float deg);
};

#endif
