#ifndef ArduinoMsgCodec_h
#define ArduinoMsgCodec_h

/*******************************************************************
 * MRLCOMM FUNCTION GENERATED INTERFACE
 * these defines are generated with :
 *							arduinoMsgs.schema
 * 							ArduinoMsgGenerator
 * 							src\resource\Arduino\generate\ArduinoMsgCodec.template.h
 */

#define MRLCOMM_VERSION			%MRLCOMM_VERSION%
#define MAGIC_NUMBER            170 // 10101010
#define MAX_MSG_SIZE			64

%cppDeviceTypes%

%defines%


/*******************************************************************
 * BOARD TYPE
 */
#define BOARD_TYPE_ID_UNKNOWN 	0
#define BOARD_TYPE_ID_MEGA    	1
#define BOARD_TYPE_ID_UNO     	2
#define BOARD_TYPE_ID_MEGA_ADK	3
#define BOARD_TYPE_ID_NANO     	4
#define BOARD_TYPE_ID_PRO_MINI	5

#if defined(ARDUINO_AVR_MEGA2560) || defined(ARDUINO_AVR_ADK)
  #define BOARD BOARD_TYPE_ID_MEGA
#elif defined(ARDUINO_AVR_UNO)
  #define BOARD BOARD_TYPE_ID_UNO
#elif defined(ARDUINO_AVR_ADK)
  #define BOARD BOARD_TYPE_ID_MEGA_ADK
#elif defined(ARDUINO_AVR_NANO)
  #define BOARD BOARD_TYPE_ID_NANO
#elif defined(ARDUINO_AVR_PRO)
  #define BOARD BOARD_TYPE_ID_PRO_MINI
#else
  #define BOARD BOARD_TYPE_ID_UNKNOWN
#endif

#endif
