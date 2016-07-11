#ifndef ArduinoMsgCodec_h
#define ArduinoMsgCodec_h

// ----- MRLCOMM FUNCTION GENERATED INTERFACE BEGIN -----------
///// INO GENERATED DEFINITION BEGIN //////
// {publishMRLCommError Integer}
#define PUBLISH_MRLCOMM_ERROR    1
// {getVersion}
#define GET_VERSION   2
// {publishVersion Integer}
#define PUBLISH_VERSION   3
// {analogReadPollingStart Integer Integer}
#define ANALOG_READ_POLLING_START   4
// {analogReadPollingStop int}
#define ANALOG_READ_POLLING_STOP    5
// {analogWrite int int}
#define ANALOG_WRITE    6
// {createI2cDevice I2CControl int int}
#define CREATE_I2C_DEVICE   7
// {deviceAttach DeviceControl Object[]}
#define DEVICE_ATTACH   8
// {deviceDetach DeviceControl}
#define DEVICE_DETACH   9
// {digitalReadPollingStart Integer Integer}
#define DIGITAL_READ_POLLING_START    10
// {digitalReadPollingStop int}
#define DIGITAL_READ_POLLING_STOP   11
// {digitalWrite int int}
#define DIGITAL_WRITE   12
// {fixPinOffset Integer}
#define FIX_PIN_OFFSET    13
// {getBoardInfo}
#define GET_BOARD_INFO    14
// {i2cRead I2CControl int int byte[] int}
#define I2C_READ    15
// {i2cWrite I2CControl int int byte[] int}
#define I2C_WRITE   16
// {i2cWriteRead I2CControl int int byte[] int byte[] int}
#define I2C_WRITE_READ    17
// {intsToString int[] int int}
#define INTS_TO_STRING    18
// {isAttached}
#define IS_ATTACHED   19
// {motorMove MotorControl}
#define MOTOR_MOVE    20
// {motorMoveTo MotorControl}
#define MOTOR_MOVE_TO   21
// {motorReset MotorControl}
#define MOTOR_RESET   22
// {motorStop MotorControl}
#define MOTOR_STOP    23
// {neoPixelWriteMatrix NeoPixel List}
#define NEO_PIXEL_WRITE_MATRIX    24
// {pinMode int String}
#define PIN_MODE    25
// {publishAttachedDevice String}
#define PUBLISH_ATTACHED_DEVICE   26
// {publishBoardInfo MrlCommStatus}
#define PUBLISH_BOARD_INFO    27
// {publishDebug String}
#define PUBLISH_DEBUG   28
// {publishMessageAck}
#define PUBLISH_MESSAGE_ACK   29
// {publishPin Pin}
#define PUBLISH_PIN   30
// {publishPulse Long}
#define PUBLISH_PULSE   31
// {publishPulseStop Integer}
#define PUBLISH_PULSE_STOP    32
// {publishSensorData Object}
#define PUBLISH_SENSOR_DATA   33
// {publishServoEvent Integer}
#define PUBLISH_SERVO_EVENT   34
// {publishStatus Long Integer}
#define PUBLISH_STATUS    35
// {publishTrigger Pin}
#define PUBLISH_TRIGGER   36
// {pulse int int int int}
#define PULSE   37
// {pulseStop}
#define PULSE_STOP    38
// {releaseI2cDevice I2CControl int int}
#define RELEASE_I2C_DEVICE    39
// {sensorActivate SensorControl Object[]}
#define SENSOR_ACTIVATE   40
// {sensorDeactivate SensorControl}
#define SENSOR_DEACTIVATE   41
// {sensorPollingStart String}
#define SENSOR_POLLING_START    42
// {sensorPollingStop String}
#define SENSOR_POLLING_STOP   43
// {servoAttach ServoControl int}
#define SERVO_ATTACH    44
// {servoDetach ServoControl}
#define SERVO_DETACH    45
// {servoEventsEnabled ServoControl boolean}
#define SERVO_EVENTS_ENABLED    46
// {servoSetSpeed ServoControl}
#define SERVO_SET_SPEED   47
// {servoSweepStart ServoControl}
#define SERVO_SWEEP_START   48
// {servoSweepStop ServoControl}
#define SERVO_SWEEP_STOP    49
// {servoWrite ServoControl}
#define SERVO_WRITE   50
// {servoWriteMicroseconds ServoControl int}
#define SERVO_WRITE_MICROSECONDS    51
// {setDebounce int}
#define SET_DEBOUNCE    52
// {setDebug boolean}
#define SET_DEBUG   53
// {setDigitalTriggerOnly Boolean}
#define SET_DIGITAL_TRIGGER_ONLY    54
// {setLoadTimingEnabled boolean}
#define SET_LOAD_TIMING_ENABLED   55
// {setPWMFrequency Integer Integer}
#define SET_PWMFREQUENCY    56
// {setSampleRate int}
#define SET_SAMPLE_RATE   57
// {setSerialRate int}
#define SET_SERIAL_RATE   58
// {setTrigger int int int}
#define SET_TRIGGER   59
// {softReset}
#define SOFT_RESET    60
///// INO GENERATED DEFINITION END //////

// serial protocol functions
#define MAGIC_NUMBER            170 // 10101010

// ----- MRLCOMM FUNCTION GENERATED INTERFACE END -----------

/***********************************************************************
 * BOARD TYPE
 */
#define BOARD_TYPE_ID_UNKNOWN 0
#define BOARD_TYPE_ID_MEGA    1
#define BOARD_TYPE_ID_UNO     2

#if defined(ARDUINO_AVR_MEGA2560)
  #define BOARD BOARD_TYPE_ID_MEGA
#elif defined(ARDUINO_AVR_UNO)
  #define BOARD BOARD_TYPE_ID_UNO
#else
  #define BOARD BOARD_TYPE_ID_UNKNOWN
#endif


#endif
