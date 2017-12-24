#ifndef ArduinoMsgCodec_h
#define ArduinoMsgCodec_h

/*******************************************************************
 * MRLCOMM FUNCTION GENERATED INTERFACE
 * these defines are generated with :
 *							arduinoMsgs.schema
 * 							ArduinoMsgGenerator
 * 							src\resource\Arduino\generate\ArduinoMsgCodec.template.h
 */

#define MRLCOMM_VERSION			57
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
// > customMsg/[] msg
#define CUSTOM_MSG 12
// < publishCustomMsg/[] msg
#define PUBLISH_CUSTOM_MSG 13
// > deviceDetach/deviceId
#define DEVICE_DETACH 14
// > i2cBusAttach/deviceId/i2cBus
#define I2C_BUS_ATTACH 15
// > i2cRead/deviceId/deviceAddress/size
#define I2C_READ 16
// > i2cWrite/deviceId/deviceAddress/[] data
#define I2C_WRITE 17
// > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
#define I2C_WRITE_READ 18
// < publishI2cData/deviceId/[] data
#define PUBLISH_I2C_DATA 19
// > neoPixelAttach/deviceId/pin/b32 numPixels
#define NEO_PIXEL_ATTACH 20
// > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
#define NEO_PIXEL_SET_ANIMATION 21
// > neoPixelWriteMatrix/deviceId/[] buffer
#define NEO_PIXEL_WRITE_MATRIX 22
// > analogWrite/pin/value
#define ANALOG_WRITE 23
// > digitalWrite/pin/value
#define DIGITAL_WRITE 24
// > disablePin/pin
#define DISABLE_PIN 25
// > disablePins
#define DISABLE_PINS 26
// > pinMode/pin/mode
#define PIN_MODE 27
// < publishDebug/str debugMsg
#define PUBLISH_DEBUG 28
// < publishPinArray/[] data
#define PUBLISH_PIN_ARRAY 29
// > setTrigger/pin/triggerValue
#define SET_TRIGGER 30
// > setDebounce/pin/delay
#define SET_DEBOUNCE 31
// > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity/str name
#define SERVO_ATTACH 32
// > servoAttachPin/deviceId/pin
#define SERVO_ATTACH_PIN 33
// > servoDetachPin/deviceId
#define SERVO_DETACH_PIN 34
// > servoSetVelocity/deviceId/b16 velocity
#define SERVO_SET_VELOCITY 35
// > servoSweepStart/deviceId/min/max/step
#define SERVO_SWEEP_START 36
// > servoSweepStop/deviceId
#define SERVO_SWEEP_STOP 37
// > servoMoveToMicroseconds/deviceId/b16 target
#define SERVO_MOVE_TO_MICROSECONDS 38
// > servoSetAcceleration/deviceId/b16 acceleration
#define SERVO_SET_ACCELERATION 39
// < publishServoEvent/deviceId/eventType/b16 currentPos/b16 targetPos
#define PUBLISH_SERVO_EVENT 40
// > serialAttach/deviceId/relayPin
#define SERIAL_ATTACH 41
// > serialRelay/deviceId/[] data
#define SERIAL_RELAY 42
// < publishSerialData/deviceId/[] data
#define PUBLISH_SERIAL_DATA 43
// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
#define ULTRASONIC_SENSOR_ATTACH 44
// > ultrasonicSensorStartRanging/deviceId
#define ULTRASONIC_SENSOR_START_RANGING 45
// > ultrasonicSensorStopRanging/deviceId
#define ULTRASONIC_SENSOR_STOP_RANGING 46
// < publishUltrasonicSensorData/deviceId/b16 echoTime
#define PUBLISH_ULTRASONIC_SENSOR_DATA 47
// > setAref/b16 type
#define SET_AREF 48
// > motorAttach/deviceId/type/[] pins
#define MOTOR_ATTACH 49
// > motorMove/deviceId/pwr
#define MOTOR_MOVE 50
// > motorMoveTo/deviceId/pos
#define MOTOR_MOVE_TO 51



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
