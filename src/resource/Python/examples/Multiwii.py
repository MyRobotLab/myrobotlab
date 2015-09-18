#define POLL_PERIOD 20
serial = Runtime.start("serial","Serial")

COMPORT= "COM19"
BAUDRATE = 9600
#define MSP_SET_RAW_RC        200
#define MSP_SET_RAW_RC_LENGTH 16

RC_MIN = 1000
RC_MID = 1500
RC_MAX = 2000

ROLL     =  0
PITCH    =  1
YAW      =  2
THROTTLE =  3
AUX1     =  4
AUX2     =  5
AUX3     =  6
AUX4     =  7

#RC signals to send to the quad
#format: { roll, throttle, yaw, pitch, 0, 0, 0, 0 }
uint16_t rc_signals[8] = { 1234 }; 

#Buffer for storing the serializes byte form of the RC signals
uint8_t rc_bytes[16] = { 0 };

serial.connect(COMPORT,BAUDRATE,8,1,0);

send_msp(MSP_SET_RAW_RC, rc_bytes, MSP_SET_RAW_RC_LENGTH);

def arm():
  msgType = 200 # MSP_SET_RAW_RC
  
  # header $M<
  serial.write('$')
  serial.write('M')
  serial.write('<')
  serial.write(msgType)
  serial.write(MsgLength??)
  
  # start checksum
  checksum = 0
  checksum ^= (16 & 0xFF);
  checksum ^= (200) # msp

  # msg data - LSB first 2 byte uint_8
  serial.write(1500) # min roll 
  checksum ^= (1500)
  serial.write(1500 >> 8) # min roll 
  checksum ^= (1500 >> 8)

  serial.write(1000) # min throttle 
  checksum ^= (1000)
  serial.write(1000 >> 8) # min throttle 

  serial.write(2000) # max yaw
  serial.write(2000 >> 8) # max yaw

  serial.write(1500) # min pitch
  serial.write(1500 >> 8) # min pitch

  serial.write(1000) # aux1
  serial.write(1000 >> 8) # aux1

  serial.write(1000) # aux2
  serial.write(1000 >> 8) # aux2

  serial.write(1000) # aux3
  serial.write(1000 >> 8) # aux3

  serial.write(1000) # aux4
  serial.write(1000 >> 8) # aux4
  
  serial.write(checksum)
  
  # rc_signals[THROTTLE] = RC_MIN;
  # rc_signals[YAW] = RC_MAX;
  # rc_signals[PITCH] = RC_MID;
  # rc_signals[ROLL] = RC_MID;
  # rc_signals[AUX1] = RC_MIN;
  # rc_signals[AUX2] = RC_MIN;
  
arm()
