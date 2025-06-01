#ifndef ArduinoMsgCodec_h
#define ArduinoMsgCodec_h

/*******************************************************************
 * MRLCOMM FUNCTION GENERATED INTERFACE
 * these defines are generated with :
 *							arduinoMsgs.schema
 * 							ArduinoMsgGenerator
 * 							src\resource\Arduino\generate\ArduinoMsgCodec.template.h
 */

#define MRLCOMM_VERSION			68
#define MAGIC_NUMBER            170 // 10101010
#define MAX_MSG_SIZE			64

#define DEVICE_TYPE_UNKNOWN    0
#define DEVICE_TYPE_ARDUINO    1
#define DEVICE_TYPE_ULTRASONICSENSOR    2
#define DEVICE_TYPE_STEPPER    3
#define DEVICE_TYPE_MOTOR    4
#define DEVICE_TYPE_SERVO    5
#define DEVICE_TYPE_SERIAL    6
#define DEVICE_TYPE_I2C    7
#define DEVICE_TYPE_NEOPIXEL    8
#define DEVICE_TYPE_ENCODER    9


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
// > neoPixelAttach/deviceId/pin/b16 numPixels/depth
#define NEO_PIXEL_ATTACH 20
// > neoPixelSetAnimation/deviceId/animation/red/green/blue/white/b32 wait_ms
#define NEO_PIXEL_SET_ANIMATION 21
// > neoPixelWriteMatrix/deviceId/[] buffer
#define NEO_PIXEL_WRITE_MATRIX 22
// > neoPixelFill/deviceId/b16 address/b16 count/red/green/blue/white
#define NEO_PIXEL_FILL 23
// > neoPixelSetBrightness/deviceId/brightness
#define NEO_PIXEL_SET_BRIGHTNESS 24
// > neoPixelClear/deviceId
#define NEO_PIXEL_CLEAR 25
// > analogWrite/pin/value
#define ANALOG_WRITE 26
// > digitalWrite/pin/value
#define DIGITAL_WRITE 27
// > disablePin/pin
#define DISABLE_PIN 28
// > disablePins
#define DISABLE_PINS 29
// > pinMode/pin/mode
#define PIN_MODE 30
// < publishDebug/str debugMsg
#define PUBLISH_DEBUG 31
// < publishPinArray/[] data
#define PUBLISH_PIN_ARRAY 32
// > setTrigger/pin/triggerValue
#define SET_TRIGGER 33
// > setDebounce/pin/delay
#define SET_DEBOUNCE 34
// > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity/str name
#define SERVO_ATTACH 35
// > servoAttachPin/deviceId/pin
#define SERVO_ATTACH_PIN 36
// > servoDetachPin/deviceId
#define SERVO_DETACH_PIN 37
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
// < publishServoEvent/deviceId/eventType/b16 currentPos/b16 targetPos
#define PUBLISH_SERVO_EVENT 43
// > serialAttach/deviceId/relayPin
#define SERIAL_ATTACH 44
// > serialRelay/deviceId/[] data
#define SERIAL_RELAY 45
// < publishSerialData/deviceId/[] data
#define PUBLISH_SERIAL_DATA 46
// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
#define ULTRASONIC_SENSOR_ATTACH 47
// > ultrasonicSensorStartRanging/deviceId
#define ULTRASONIC_SENSOR_START_RANGING 48
// > ultrasonicSensorStopRanging/deviceId
#define ULTRASONIC_SENSOR_STOP_RANGING 49
// < publishUltrasonicSensorData/deviceId/b16 echoTime
#define PUBLISH_ULTRASONIC_SENSOR_DATA 50
// > setAref/b16 type
#define SET_AREF 51
// > motorAttach/deviceId/type/[] pins
#define MOTOR_ATTACH 52
// > motorMove/deviceId/pwr
#define MOTOR_MOVE 53
// > motorMoveTo/deviceId/pos
#define MOTOR_MOVE_TO 54
// > encoderAttach/deviceId/type/pin
#define ENCODER_ATTACH 55
// > setZeroPoint/deviceId
#define SET_ZERO_POINT 56
// < publishEncoderData/deviceId/b16 position
#define PUBLISH_ENCODER_DATA 57
// < publishMrlCommBegin/version
#define PUBLISH_MRL_COMM_BEGIN 58
// > servoStop/deviceId
#define SERVO_STOP 59



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
