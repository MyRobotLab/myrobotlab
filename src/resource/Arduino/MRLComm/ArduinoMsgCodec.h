#ifndef ArduinoMsgCodec_h
#define ArduinoMsgCodec_h

/*******************************************************************
 * MRLCOMM FUNCTION GENERATED INTERFACE
 * these defines are generated with :
 *							arduinoMsgs.schema
 * 							ArduinoMsgGenerator
 * 							src\resource\Arduino\generate\ArduinoMsgCodec.template.h
 */

#define MRLCOMM_VERSION			53
#define MAGIC_NUMBER            170 // 10101010
#define MAX_MSG_SIZE			64

#define DEVICE_TYPE_UNKNOWN		0
#define DEVICE_TYPE_ARDUINO		1
#define DEVICE_TYPE_ULTRASONICSENSOR		2
#define DEVICE_TYPE_STEPPER		3
#define DEVICE_TYPE_MOTOR		4
#define DEVICE_TYPE_SERVO		5
#define DEVICE_TYPE_SERIAL		6
#define DEVICE_TYPE_I2C		7
#define DEVICE_TYPE_NEOPIXEL		8


// < publishMRLCommError/str errorMsg
#define PUBLISH_MRLCOMM_ERROR 1
// > getBoardInfo
#define GET_BOARD_INFO 2
// < publishBoardInfo/version/boardType/b16 microsPerLoop/b16 sram/activePins/[] deviceSummary
#define PUBLISH_BOARD_INFO 3
// > enablePin/address/type/b16 rate
#define ENABLE_PIN 4
// > setDebug/bool enabled
#define SET_DEBUG 5
// > setSerialRate/b32 rate
#define SET_SERIAL_RATE 6
// > softReset
#define SOFT_RESET 7
// > enableAck/bool enabled
#define ENABLE_ACK 8
// < publishAck/function
#define PUBLISH_ACK 9
// > echo/f32 myFloat/myByte/f32 secondFloat
#define ECHO 10
// < publishEcho/f32 myFloat/myByte/f32 secondFloat
#define PUBLISH_ECHO 11
// > controllerAttach/serialPort
#define CONTROLLER_ATTACH 12
// > customMsg/[] msg
#define CUSTOM_MSG 13
// < publishCustomMsg/[] msg
#define PUBLISH_CUSTOM_MSG 14
// > deviceDetach/deviceId
#define DEVICE_DETACH 15
// > i2cBusAttach/deviceId/i2cBus
#define I2C_BUS_ATTACH 16
// > i2cRead/deviceId/deviceAddress/size
#define I2C_READ 17
// > i2cWrite/deviceId/deviceAddress/[] data
#define I2C_WRITE 18
// > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
#define I2C_WRITE_READ 19
// < publishI2cData/deviceId/[] data
#define PUBLISH_I2C_DATA 20
// > neoPixelAttach/deviceId/pin/b32 numPixels
#define NEO_PIXEL_ATTACH 21
// > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
#define NEO_PIXEL_SET_ANIMATION 22
// > neoPixelWriteMatrix/deviceId/[] buffer
#define NEO_PIXEL_WRITE_MATRIX 23
// > analogWrite/pin/value
#define ANALOG_WRITE 24
// > digitalWrite/pin/value
#define DIGITAL_WRITE 25
// > disablePin/pin
#define DISABLE_PIN 26
// > disablePins
#define DISABLE_PINS 27
// > pinMode/pin/mode
#define PIN_MODE 28
// < publishAttachedDevice/deviceId/str deviceName
#define PUBLISH_ATTACHED_DEVICE 29
// < publishDebug/str debugMsg
#define PUBLISH_DEBUG 30
// < publishPinArray/[] data
#define PUBLISH_PIN_ARRAY 31
// > setTrigger/pin/triggerValue
#define SET_TRIGGER 32
// > setDebounce/pin/delay
#define SET_DEBOUNCE 33
// > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity
#define SERVO_ATTACH 34
// > servoAttachPin/deviceId/pin
#define SERVO_ATTACH_PIN 35
// > servoDetachPin/deviceId
#define SERVO_DETACH_PIN 36
// > servoSetMaxVelocity/deviceId/b16 maxVelocity
#define SERVO_SET_MAX_VELOCITY 37
// > servoSetVelocity/deviceId/b16 velocity
#define SERVO_SET_VELOCITY 38
// > servoSweepStart/deviceId/min/max/step
#define SERVO_SWEEP_START 39
// > servoSweepStop/deviceId
#define SERVO_SWEEP_STOP 40
// > servoMoveToMicroseconds/deviceId/b16 target
#define SERVO_MOVE_TO_MICROSECONDS 41
// > servoSetAcceleration/deviceId/b16 acceleration
#define SERVO_SET_ACCELERATION 42
// > serialAttach/deviceId/relayPin
#define SERIAL_ATTACH 43
// > serialRelay/deviceId/[] data
#define SERIAL_RELAY 44
// < publishSerialData/deviceId/[] data
#define PUBLISH_SERIAL_DATA 45
// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
#define ULTRASONIC_SENSOR_ATTACH 46
// > ultrasonicSensorStartRanging/deviceId
#define ULTRASONIC_SENSOR_START_RANGING 47
// > ultrasonicSensorStopRanging/deviceId
#define ULTRASONIC_SENSOR_STOP_RANGING 48
// < publishUltrasonicSensorData/deviceId/b16 echoTime
#define PUBLISH_ULTRASONIC_SENSOR_DATA 49



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
