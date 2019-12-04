#ifndef MrlAmt203Encoder_h
#define MrlAmt203Encoder_h


/**
 * AMT203 Encoder by CUI.
 * This is a capactive absolute position 12 bit encoder.
 * It communicates via SPI.
 *
 */
class MrlAmt203Encoder : public Device {

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
    MrlAmt203Encoder(int deviceId);
    ~MrlAmt203Encoder();
    bool attach(byte pin);
    void update();
    void setZeroPoint();
    void publishEncoderData(float deg);
};

#endif
