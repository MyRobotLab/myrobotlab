#ifndef MrlComm_h
#define MrlComm_h

#include "Arduino.h"
#include "ArduinoMsgCodec.h"
#include "MrlMsg.h"
#include "LinkedList.h"
#include "MrlServo.h"
#include "Device.h"
#include "MrlI2cBus.h"
#include "MrlNeopixel.h"

/***********************************************************************
 * Class MrlComm
 * 
*/
class MrlComm{
  private:
    /**
     * "global var"
     */
    // The mighty device List.  This contains all active devices that are attached to the arduino.
    LinkedList<Device*> deviceList;
    // MRLComm message buffer and current count from serial port ( MAGIC | MSGSIZE | FUNCTION | PAYLOAD ...
    unsigned char ioCmd[MAX_MSG_SIZE];  // message buffer for all inbound messages
    unsigned char* config;
    // performance metrics  and load timing
    // global debug setting, if set to true publishDebug will write to the serial port.
    bool debug;
    int byteCount;
    int msgSize;
  public:
    unsigned long loopCount; // main loop count
    bool loadTimingEnabled;
    int loadTimingModulus; // the frequency in which to report the load timing metrics (in number of main loops)
    unsigned long lastMicros; // timestamp of last loop (if stats enabled.)
    MrlComm(){
      softReset();
      byteCount = 0;
    }
/***********************************************************************
 * UTILITY METHODS BEGIN
 */
    void softReset(){
      while(deviceList.size()>0){
        delete deviceList.pop();
      }
      //resetting var to default
      loopCount = 0;
      loadTimingModulus = 1000;
      loadTimingEnabled = false;
      Device::nextDeviceId = 0;
      debug = false;
    }
    /***********************************************************************
     * UPDATE STATUS 
     * This function updates the average time it took to run the main loop
     * and reports it back with a publishStatus MRLComm message
     *
     * TODO: avgTiming could be 0 if loadTimingModule = 0 ?!
     */
    void updateStatus() {
      // protect against a divide by zero in the division.
      unsigned long avgTiming = 0;
      if (loadTimingModulus != 0) {
        avgTiming = (micros() - lastMicros) / loadTimingModulus;
      } 
      // report load time
      if (loadTimingEnabled && (loopCount%loadTimingModulus == 0)) {
        // send the average loop timing.
        publishStatus(avgTiming, getFreeRam());
      }
      // update the timestamp of this update.
      lastMicros = micros();
    }
    int getFreeRam() {
      // KW: In the future the arduino might have more than an 32/64k of ram. an int might not be enough here to return.
      extern int __heap_start, *__brkval;
      int v;
      return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval);
    }
/***********************************************************************
 * PUBLISH DEVICES BEGIN
 * 
 * All serial IO should happen here to publish a MRLComm message.
 * TODO: move all serial IO into a controlled place this this below...
 * TODO: create MRLCommMessage class that can just send itself!
 * 
 */
    /**
     * Publish the MRLComm message
     * MAGIC_NUMBER|2|MRLCOMM_VERSION
     */
    void publishVersion() {
      MrlMsg msg(PUBLISH_VERSION);
      msg.addData(MRLCOMM_VERSION);
      msg.sendMsg();
    }
    /**
     * publishBoardInfo()
     * MAGIC_NUMBER|2|PUBLISH_BOARD_INFO|BOARD
     * return the board type (mega/uno) that can use in javaland for the pin layout
     */
    void publishBoardInfo() {
      MrlMsg msg(PUBLISH_BOARD_INFO);
      msg.addData(BOARD);
      msg.sendMsg();
    }
    /**
     * Publish Debug - return a text debug message back to the java based arduino service in MRL
     * MAGIC_NUMBER|1+MSG_LENGTH|MESSAGE_BYTES
     * 
     * This method will publish a string back to the Arduino service for debugging purproses.
     * 
     */
    void publishDebug(String message) {
      if (debug) {
        // NOTE-KW:  If this method gets called excessively I have seen memory corruption in the 
        // arduino where it seems to be getting a null string passed in as "message"
        // very very very very very odd..  I suspect a bug in the arduino hardware/software
        MrlMsg msg(PUBLISH_DEBUG);
        msg.addData(message);
        msg.sendMsg();
      }
    }
    /**
     * send an error message/code back to MRL.
     * MAGIC_NUMBER|2|PUBLISH_MRLCOMM_ERROR|ERROR_CODE
     */
    // KW: remove this, force an error message.
    void publishError(int type) {
      MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
      msg.addData(type);
      msg.sendMsg();
    }
    /**
     * Send an error message along with the error code
     * 
     */
    void publishError(int type, String message) {
      MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
      msg.addData(type);
      msg.addData(message);
      msg.sendMsg();
    }
    /**
     * Publish the acknowledgement of the command received and processed.
     * MAGIC_NUMBER|2|PUBLISH_MESSAGE_ACK|FUNCTION
     */
    void publishCommandAck() {
      MrlMsg msg(PUBLISH_MESSAGE_ACK);
      // the function that we're ack-ing
      msg.addData(ioCmd[0]);
      msg.sendMsg();
    }
     /**
     * PUBLISH_ATTACHED_DEVICE
     * MSG STRUCTURE
     * PUBLISH_ATTACHED_DEVICE | NEW_DEVICE_INDEX | NAME_STR_SIZE | NAME
     *
     */
    void publishAttachedDevice(int id, int nameSize, int namePos){
      MrlMsg msg(PUBLISH_ATTACHED_DEVICE,id);
      msg.addData(ioCmd+namePos,nameSize,true);
      msg.sendMsg();
    }
    /**
     * publishStatus
     * This method is for performance profiling, it returns back the amount of time
     * it took to run the loop() method and how much memory was free after that 
     * loop method ran.
     * 
     * MAGIC_NUMBER|7|[loadTime long0,1,2,3]|[freeMemory int0,1]
     */
    void publishStatus(unsigned long loadTime, int freeMemory) {
      MrlMsg msg(PUBLISH_STATUS);
      msg.addData(loadTime);
      msg.addData16(freeMemory);
      msg.sendMsg();
    }
/***********************************************************************
 * SERIAL METHODS BEGIN
 */
    void readCommand(){
      if (getCommand()) {
        processCommand();
      }
    }
    /**
     * getCommand() - This is the main method to read new data from the serial port, 
     * when a full mrlcomm message is read from the serial port.
     * return values: true if the serial port read a full mrlcomm command
     *                false if the serial port is still waiting on a command.
     */
    bool getCommand() {
      static int byteCount;
      static int msgSize;
      // handle serial data begin
      int bytesAvailable = Serial.available();
      if (bytesAvailable > 0) {
        publishDebug("RXBUFF:" + String(bytesAvailable));
        // now we should loop over the available bytes .. not just read one by one.
        for (int i = 0 ; i < bytesAvailable; i++) {
          // read the incoming byte:
          unsigned char newByte = Serial.read();
          publishDebug("RX:" + String(newByte));
          ++byteCount;
          // checking first byte - beginning of message?
          if (byteCount == 1 && newByte != MAGIC_NUMBER) {
            publishError(ERROR_SERIAL);
            // reset - try again
            byteCount = 0;
            // return false;
          }
          if (byteCount == 2) {
            // get the size of message
            // todo check msg < 64 (MAX_MSG_SIZE)
            if (newByte > 64){
              // TODO - send error back
              byteCount = 0;
              continue; // GroG - I guess  we continue now vs return false on error conditions?
            }
            msgSize = newByte;
          }
          if (byteCount > 2) {
            // fill in msg data - (2) headbytes -1 (offset)
            ioCmd[byteCount - 3] = newByte;
          }
          // if received header + msg
          if (byteCount == 2 + msgSize) {
            // we've reach the end of the command, just return true .. we've got it
            byteCount=0;
            return true;
          }
        }
      } // if Serial.available
      // we only partially read a command.  (or nothing at all.)
      return false;
          
    }
    // This function will switch the current command and call
    // the associated function with the command
    /**
     * processCommand() - once the main loop has read an mrlcomm message from the 
     * serial port, this method will be called.
     */
    void processCommand() {
      // FIXME - all case X: should have scope operator { } !
      switch (ioCmd[0]) {
      // === system pass through begin ===
      case DIGITAL_WRITE:
        digitalWrite(ioCmd[1], ioCmd[2]);
        break;
      case ANALOG_WRITE:{
        analogWrite(ioCmd[1], ioCmd[2]);
        break;
      }
      case PIN_MODE:{
        pinMode(ioCmd[1], ioCmd[2]);
        break;
      }
      case SERVO_ATTACH:{
        int pin = ioCmd[2];
        publishDebug("SERVO_ATTACH " + String(pin));
        MrlServo* servo = (MrlServo*)getDevice(ioCmd[1]);
        servo->attach(pin);
        publishDebug(F("SERVO_ATTACHED"));
          break;
      }
      case SERVO_SWEEP_START:
        //startSweep(min,max,step)
        ((MrlServo*)getDevice(ioCmd[1]))->startSweep(ioCmd[2],ioCmd[3],ioCmd[4]);
        break;
      case SERVO_SWEEP_STOP:
        ((MrlServo*)getDevice(ioCmd[1]))->stopSweep();
        break;
      case SERVO_EVENTS_ENABLED:
        // PUBLISH_SERVO_EVENT seem to do the same thing
        //servoEventsEnabled();
        break;
      case SERVO_WRITE:
        ((MrlServo*)getDevice(ioCmd[1]))->servoWrite(ioCmd[2]);
        break;
      case PUBLISH_SERVO_EVENT:
        ((MrlServo*)getDevice(ioCmd[1]))->servoEventEnabled(ioCmd[2]);
        break;
      case SERVO_WRITE_MICROSECONDS:
        ((MrlServo*)getDevice(ioCmd[1]))->servoWriteMicroseconds(ioCmd[2]);
        break;
      case SERVO_SET_SPEED:
        ((MrlServo*)getDevice(ioCmd[1]))->setSpeed(ioCmd[2]);
        break;
      case SERVO_DETACH:{
        publishDebug("SERVO_DETACH " + String(ioCmd[1]));
        ((MrlServo*)getDevice(ioCmd[1]))->detach();
        publishDebug("SERVO_DETACHED");
        break;
      }
      case SET_LOAD_TIMING_ENABLED:
        loadTimingEnabled = ioCmd[1];
        //loadTimingModulus = ioCmd[2];
        loadTimingModulus = 1;
        break;
      case SET_PWMFREQUENCY:
        setPWMFrequency(ioCmd[1], ioCmd[2]);
        break;
      case ANALOG_READ_POLLING_START:
        //analogReadPollingStart();
        break;
      case ANALOG_READ_POLLING_STOP:
        //analogReadPollingStop();
        break;
      case DIGITAL_READ_POLLING_START:
        //digitalReadPollingStart();
        break;
      case DIGITAL_READ_POLLING_STOP:
        //digitalReadPollingStop();
          break;
      case PULSE:
        //((MrlPulse*)getDevice(ioCmd[1]))->pulse(ioCmd);
        break;
      case PULSE_STOP:
        //((MrlPulse*)getDevice(ioCmd[1]))->pulseStop();
        break;
      case SET_TRIGGER:
        //setTrigger();
        break;
      case SET_DEBOUNCE:
        //setDebounce();
        break;
      case SET_DIGITAL_TRIGGER_ONLY:
        //setDigitalTriggerOnly();
        break;
      case SET_SERIAL_RATE:
        setSerialRate();
        break;
      case GET_VERSION:
        publishVersion();
        break;
      case SET_SAMPLE_RATE:
        //setSampleRate();
        break;
      case SOFT_RESET:
        softReset();
        break;
      case SENSOR_POLLING_START:
        //sensorPollingStart();
        break;
      case DEVICE_ATTACH:
        deviceAttach();
      break;
      case DEVICE_DETACH:
        deviceDetach(ioCmd[1]);
      break;
      case SENSOR_POLLING_STOP:
        //sensorPollingStop();
        break;
      // Start of i2c read and writes
      case I2C_READ:
          ((MrlI2CBus*)getDevice(ioCmd[1]))->i2cRead(&ioCmd[0]);
        break;
      case I2C_WRITE:
          ((MrlI2CBus*)getDevice(ioCmd[1]))->i2cWrite(&ioCmd[0]);
        break;
      case I2C_WRITE_READ:
          ((MrlI2CBus*)getDevice(ioCmd[1]))->i2cWriteRead(&ioCmd[0]);
        break;
      case SET_DEBUG:
        debug = ioCmd[1];
        if (debug) {
          publishDebug(F("Debug logging enabled."));
        }
        break;
      case GET_BOARD_INFO:
        publishBoardInfo();
        break;
      case NEO_PIXEL_WRITE_MATRIX:
          ((MrlNeopixel*)getDevice(ioCmd[1]))->neopixelWriteMatrix(ioCmd);
          break;
      default:
        publishError(ERROR_UNKOWN_CMD);
        break;
      } // end switch
      // ack that we got a command (should we ack it first? or after we process the command?)
      publishCommandAck();
      // reset command buffer to be ready to receive the next command.
      // KW: we should only need to set the byteCount back to zero. clearing this array is just for safety sake i guess?
      // GR: yup
      memset(ioCmd, 0, sizeof(ioCmd));
      //byteCount = 0;
    } // process Command
    /***********************************************************************
     * CONTROL METHODS BEGIN
     * These methods map one to one for each MRLComm command that comes in.
     * 
     * TODO - add text api
     */
    // SET_PWMFREQUENCY
    void setPWMFrequency(int address, int prescalar) {
      // FIXME - different boards have different timers
      // sets frequency of pwm of analog
      // FIXME - us ifdef appropriate uC which
      // support these clocks TCCR0B
      int clearBits = 0x07;
      if (address == 0x25) {
        TCCR0B &= ~clearBits;
        TCCR0B |= prescalar;
      } else if (address == 0x2E) {
        TCCR1B &= ~clearBits;
        TCCR1B |= prescalar;
      } else if (address == 0xA1) {
        TCCR2B &= ~clearBits;
        TCCR2B |= prescalar;
      }
    }
    // SET_SERIAL_RATE
    void setSerialRate() {
      Serial.end();
      delay(500);
      Serial.begin(ioCmd[1]);
    }
    /**********************************************************************
     * ATTACH DEVICES BEGIN
     *
     *<pre>
     *
     * MSG STRUCTURE
     *                    |<-- ioCmd starts here                                        |<-- config starts here
     * MAGIC_NUMBER|LENGTH|ATTACH_DEVICE|DEVICE_TYPE|NAME_SIZE|NAME .... (N)|CONFIG_SIZE|DATA0|DATA1 ...|DATA(N)
     *
     * ATTACH_DEVICE - this method id
     * DEVICE_TYPE - the mrlcomm device type we are attaching
     * NAME_SIZE - the size of the name of the service of the device we are attaching
     * NAME .... (N) - the name data
     * CONFIG_SIZE - the size of the folloing config
     * DATA0|DATA1 ...|DATA(N) - config data
     *
     *</pre>
     *
     * Device types are defined in org.myrobotlab.service.interface.Device
     * TODO crud Device operations create remove (update not needed?) delete
     * TODO probably need getDeviceId to decode id from Arduino.java - because if its
     * implemented as a ptr it will be 4 bytes - if it is a generics id
     * it could be implemented with 1 byte
     */
    void deviceAttach() {
      // TOOD:KW check free memory to see if we can attach a new device. o/w return an error!
      // we're creating a new device. auto increment it
      // TODO: consider what happens if we overflow on this auto-increment. (very unlikely. but possible)
      // we want to echo back the name
      // and send the config in a nice neat package to
      // the attach method which creates the device
      int nameSize = ioCmd[2];
    
      // get config size
      int configSizePos = 3 + nameSize;
      int configSize = ioCmd[configSizePos];
      int configPos = configSizePos + 1;
      config = ioCmd+configPos;
      // MAKE NOTE: I've chosen to have config & configPos globals
      // this is primarily to avoid the re-allocation/de-allocation of the config buffer
      // but part of me thinks it should be a local var passed into the function to avoid
      // the dangers of global var ... fortunately Arduino is single threaded
      // It also makes sense to pass in config on the constructor of a new device
      // based on device type - "you inflate the correct device with the correct config"
      // but I went on the side of globals & hopefully avoiding more memory management and fragmentation
      // CAL: change config to a pointer in ioCmd (save some memory) so config[0] = ioCmd[configPos]
    
      int type = ioCmd[1];
      Device* devicePtr = 0;
      // KW: remove this switch statement by making "attach(int[]) a virtual method on the device base class.
      // perhaps a factory to produce the devicePtr based on the deviceType..
      // currently the attach logic is embeded in the constructors ..  maybe we can make that a more official
      // lifecycle for the devices..
      // check out the make_stooge method on https://sourcemaking.com/design_patterns/factory_method/cpp/1
      // This is really how we should do this.  (methinks)
      // Cal: the make_stooge method is certainly more C++ like, but essentially do the same thing as we do, 
      // it just move this big switch to another place
    
            // GR: I agree ..  "attach" should be a universal concept of devices, yet it does not need to be implmented
            // in the constructor .. so I'm for making a virtualized attach, but just like Java-Land the attach
            // needs to have size sent in with the config since it can be variable array
            // e.g.  attach(int[] config, configSize)
    
      switch (type) {
        case SENSOR_TYPE_ANALOG_PIN_ARRAY: {
          //devicePtr = attachAnalogPinArray();
          break;
        }
        case SENSOR_TYPE_DIGITAL_PIN_ARRAY: {
          //devicePtr = attachDigitalPinArray();
          break;
        }
        case SENSOR_TYPE_PULSE: {
          //devicePtr = attachPulse();
          break;
        }
        case SENSOR_TYPE_ULTRASONIC: {
          //devicePtr = attachUltrasonic();
          break;
        }
        case DEVICE_TYPE_STEPPER: {
          //devicePtr = attachStepper();
          break;
        }
        case DEVICE_TYPE_MOTOR: {
          //devicePtr = attachMotor();
          break;
        }
        case DEVICE_TYPE_SERVO: {
          devicePtr = new MrlServo(); //no need to pass the type here
          break;
        }
        case DEVICE_TYPE_I2C: {
          devicePtr = new MrlI2CBus();
          break;
        }
        case DEVICE_TYPE_NEOPIXEL: {
          devicePtr = new MrlNeopixel();
        }
        default: {
          // TODO: publish error message
            publishDebug(F("Unknown Message Type."));
          break;
        }
      }
    
      // KW: a sort of null pointer case? TODO: maybe move this into default branch of switch above?
      if (devicePtr) {
        if(devicePtr->deviceAttach(config, configSize)) {
          addDevice(devicePtr);
          publishAttachedDevice(devicePtr->id, nameSize, 3);
        }
        else {
          publishError(ERROR_UNKOWN_SENSOR,F("DEVICE not attached"));
          delete devicePtr;
        }
      }
    }
    /**
     * deviceDetach - get the device
     * if it exists delete it and remove it from the deviceList
     */
    void deviceDetach(int id) {
      Device* device = getDevice(id);
      if (device){
        deviceList.remove(id);
        delete device;
      }
    }
    /**
     * getDevice - this method will look up a device by it's id in the device list.
     * it returns null if the device isn't found.
     */
    Device* getDevice(int id) {
      ListNode<Device*>* node=deviceList.getRoot();
      while (node != NULL) {
        if(node->data->id == id) {
          return node->data;
        }
        node = node->next;
      }
      publishError(ERROR_DOES_NOT_EXIST);
      return NULL; //returning a NULL ptr can cause runtime error
      // you'll still get a runtime error if any field, member or method not
      // defined is accessed
    }
    /**
     * This adds a device to the current set of active devices in the deviceList.
     * 
     * FIXME - G: I think dynamic array would work better
     * at least for the deviceList
     * TODO: KW: i think it's pretty dynamic now.
     * G: the nextDeviceId & Id leaves something to be desired - and the "index" does
     * not spin through the deviceList to find it .. a dynamic array of pointers would only
     * expand if it could not accomidate the current number of devices, when a device was
     * removed - the slot could be re-used by the next device request
     */
    void addDevice(Device* device) {
      deviceList.add(device);
    }
    /***********************************************************************
     * UPDATE DEVICES BEGIN
     * updateDevices updates each type of device put on the device list
     * depending on their type.
     * This method processes each loop. Typically this "back-end"
     * processing will read data from pins, or change states of non-blocking
     * pulses, or possibly regulate a motor based on pid values read from
     * pins
     */
    void updateDevices() {
      ListNode<Device*>* node = deviceList.getRoot();
      // iterate through our device list and call update on them.
      while (node != NULL) {
        node->data->update(lastMicros);
        node = node->next;
      }
    }
};

#endif
