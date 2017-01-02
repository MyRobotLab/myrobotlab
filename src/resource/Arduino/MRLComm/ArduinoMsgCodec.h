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
// < publishBoardInfo/version/boardType
#define PUBLISH_BOARD_INFO 3
// > enableBoardStatus/bool enabled
#define ENABLE_BOARD_STATUS 4
// > enablePin/address/type/b16 rate
#define ENABLE_PIN 5
// > setDebug/bool enabled
#define SET_DEBUG 6
// > setSerialRate/b32 rate
#define SET_SERIAL_RATE 7
// > softReset
#define SOFT_RESET 8
// > enableAck/bool enabled
#define ENABLE_ACK 9
// < publishAck/function
#define PUBLISH_ACK 10
// > enableHeartbeat/bool enabled
#define ENABLE_HEARTBEAT 11
// > heartbeat
#define HEARTBEAT 12
// < publishHeartbeat
#define PUBLISH_HEARTBEAT 13
// > echo/f32 myFloat/myByte/f32 secondFloat
#define ECHO 14
// < publishEcho/f32 myFloat/myByte/f32 secondFloat
#define PUBLISH_ECHO 15
// > controllerAttach/serialPort
#define CONTROLLER_ATTACH 16
// > customMsg/[] msg
#define CUSTOM_MSG 17
// < publishCustomMsg/[] msg
#define PUBLISH_CUSTOM_MSG 18
// > deviceDetach/deviceId
#define DEVICE_DETACH 19
// > i2cBusAttach/deviceId/i2cBus
#define I2C_BUS_ATTACH 20
// > i2cRead/deviceId/deviceAddress/size
#define I2C_READ 21
// > i2cWrite/deviceId/deviceAddress/[] data
#define I2C_WRITE 22
// > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
#define I2C_WRITE_READ 23
// < publishI2cData/deviceId/[] data
#define PUBLISH_I2C_DATA 24
// > neoPixelAttach/deviceId/pin/b32 numPixels
#define NEO_PIXEL_ATTACH 25
// > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
#define NEO_PIXEL_SET_ANIMATION 26
// > neoPixelWriteMatrix/deviceId/[] buffer
#define NEO_PIXEL_WRITE_MATRIX 27
// > analogWrite/pin/value
#define ANALOG_WRITE 28
// > digitalWrite/pin/value
#define DIGITAL_WRITE 29
// > disablePin/pin
#define DISABLE_PIN 30
// > disablePins
#define DISABLE_PINS 31
// > pinMode/pin/mode
#define PIN_MODE 32
// < publishAttachedDevice/deviceId/str deviceName
#define PUBLISH_ATTACHED_DEVICE 33
// < publishBoardStatus/b16 microsPerLoop/b16 sram/[] deviceSummary
#define PUBLISH_BOARD_STATUS 34
// < publishDebug/str debugMsg
#define PUBLISH_DEBUG 35
// < publishPinArray/[] data
#define PUBLISH_PIN_ARRAY 36
// > setTrigger/pin/triggerValue
#define SET_TRIGGER 37
// > setDebounce/pin/delay
#define SET_DEBOUNCE 38
// > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity
#define SERVO_ATTACH 39
// > servoAttachPin/deviceId/pin
#define SERVO_ATTACH_PIN 40
// > servoDetachPin/deviceId
#define SERVO_DETACH_PIN 41
// > servoSetMaxVelocity/deviceId/b16 maxVelocity
#define SERVO_SET_MAX_VELOCITY 42
// > servoSetVelocity/deviceId/b16 velocity
#define SERVO_SET_VELOCITY 43
// > servoSweepStart/deviceId/min/max/step
#define SERVO_SWEEP_START 44
// > servoSweepStop/deviceId
#define SERVO_SWEEP_STOP 45
// > servoMoveToMicroseconds/deviceId/b16 target
#define SERVO_MOVE_TO_MICROSECONDS 46
// > servoSetAcceleration/deviceId/b16 acceleration
#define SERVO_SET_ACCELERATION 47
// > serialAttach/deviceId/relayPin
#define SERIAL_ATTACH 48
// > serialRelay/deviceId/[] data
#define SERIAL_RELAY 49
// < publishSerialData/deviceId/[] data
#define PUBLISH_SERIAL_DATA 50
// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
#define ULTRASONIC_SENSOR_ATTACH 51
// > ultrasonicSensorStartRanging/deviceId
#define ULTRASONIC_SENSOR_START_RANGING 52
// > ultrasonicSensorStopRanging/deviceId
#define ULTRASONIC_SENSOR_STOP_RANGING 53
// < publishUltrasonicSensorData/deviceId/b16 echoTime
#define PUBLISH_ULTRASONIC_SENSOR_DATA 54



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
