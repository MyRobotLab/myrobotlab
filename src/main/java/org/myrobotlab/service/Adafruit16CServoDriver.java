/*
 * 
 *   Adafruit16CServoDriver
 *
 */

package org.myrobotlab.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Ignore;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.config.Adafruit16CServoDriverConfig;
import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.data.ServoSpeed;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * AdaFruit 16-Channel PWM / Servo Driver
 * 
 * @author GroG and Mats
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 *         https://learn.adafruit.com/16-channel-pwm-servo-driver
 */
@Ignore
public class Adafruit16CServoDriver extends Service<Adafruit16CServoDriverConfig> implements I2CControl, ServoController,
    MotorController /* , ServoStatusPublisher */ {

  /**
   * SpeedControl, calculates the next position at regular intervals to make the
   * servo move at the desired speed
   * 
   */
  public class SpeedControl extends Thread {

    volatile ServoEvent ServoEvent;
    String name;
    long now;
    long lastExecution;
    long deltaTime;

    public SpeedControl(String name) {
      super(String.format("%s.SpeedControl", name));
      ServoEvent = servoMap.get(name);
      ServoEvent.isMoving = true;

      this.name = name;
    }

    @Override
    public void run() {

      log.info("Speed control started for {}", name);
      ServoEvent = servoMap.get(name);
      log.debug("Moving from {} to {} at {} degrees/second", ServoEvent.currentOutput, ServoEvent.targetOutput, ServoEvent.velocity);
      // publishServoEvent(ServoEvent.servo, 2, ServoEvent.currentOutput);
      try {
        lastExecution = System.currentTimeMillis();
        double _velocity;
        if (ServoEvent.acceleration == -1) {
          _velocity = ServoEvent.velocity;
        } else {
          _velocity = 0;
        }
        while (ServoEvent.isMoving && ServoEvent.isEnergized) {
          now = System.currentTimeMillis();
          deltaTime = now - lastExecution;
          if (ServoEvent.acceleration != -1) {
            _velocity = _velocity + (ServoEvent.acceleration * deltaTime * 0.001);
            if (_velocity > ServoEvent.velocity) {
              _velocity = ServoEvent.velocity;
            }
          }

          if (ServoEvent.currentOutput < ServoEvent.targetOutput) { // Move
            // in
            // positive
            // direction
            ServoEvent.currentOutput += (_velocity * deltaTime) * 0.001;
            if (ServoEvent.currentOutput >= ServoEvent.targetOutput) {
              ServoEvent.currentOutput = ServoEvent.targetOutput;
              ServoEvent.isMoving = false;
            }
          } else if (ServoEvent.currentOutput > ServoEvent.targetOutput) { // Move
            // in
            // negative
            // direction
            ServoEvent.currentOutput -= (_velocity * deltaTime * 0.001);
            if (ServoEvent.currentOutput <= ServoEvent.targetOutput) {
              ServoEvent.currentOutput = ServoEvent.targetOutput;
              ServoEvent.isMoving = false;
            }
          } else {
            // We have reached the position so shutdown the thread
            ServoEvent.isMoving = false;
            log.debug("This line should not repeat");
          }
          int pulseWidthOff = SERVOMIN + (int) (ServoEvent.currentOutput * (int) ((float) SERVOMAX - (float) SERVOMIN) / (180));
          setServo(ServoEvent.pin, pulseWidthOff);
          publishServoEvent(ServoEvent.servo, 2, ServoEvent.currentOutput);
          // Sleep 100ms before sending next position
          lastExecution = now;
          log.debug("Sent {} using a {} tick at velocity {}", ServoEvent.currentOutput, deltaTime, _velocity);
          Thread.sleep(50);
        }
        publishServoEvent(ServoEvent.servo, 1, ServoEvent.currentOutput);
        log.info("publishServoEvent : {} , event {}, currentOutput {}", ServoEvent.servo.getName(), 1, ServoEvent.currentOutput);
        log.info("Shuting down SpeedControl");

      } catch (Exception e) {
        ServoEvent.isMoving = false;
        if (e instanceof InterruptedException) {
          log.debug("Shuting down SpeedControl");
        } else {
          log.error("speed control threw", e);
        }
      }
    }

  }

  public double publishServoEvent(ServoControl servo, Integer eventType, Double currentOutput) {
    if (eventType == 0) {
      broadcast("publishServoStarted", getName());
    } else if (eventType == 1) {
      broadcast("publishServoStopped", getName());
    }
    return currentOutput;
  }

  private static final long serialVersionUID = 1L;

  // Depending on your servo make, the pulse width min and max may vary, you
  // want these to be as small/large as possible without hitting the hard stop
  // for max range. You'll have to tweak them as necessary to match the servos
  // you have!
  //
  public final static int SERVOMIN = 150; // this
  // is
  // the
  // 'minimum'
  // pulse
  // length count (out of 4096)
  public final static int SERVOMAX = 600; // this
  // is
  // the
  // 'maximum'
  // pulse
  // length count (out of 4096)

  transient public I2CController controller;

  // Constant for default PWM freqency
  private static int defaultPwmFreq = 60;
  final static int minPwmFreq = 24;
  final static int maxPwmFreq = 1526;

  int pwmFreq;
  boolean pwmFreqSet = false;

  // List of possible addresses. Used by the GUI.
  public List<String> deviceAddressList = Arrays.asList("0x40", "0x41", "0x42", "0x43", "0x44", "0x45", "0x46", "0x47", "0x48", "0x49", "0x4A", "0x4B", "0x4C", "0x4D", "0x4E",
      "0x4F", "0x50", "0x51", "0x52", "0x53", "0x54", "0x55", "0x56", "0x57", "0x58", "0x59", "0x5A", "0x5B", "0x5C", "0x5D", "0x5E", "0x5F");
  // Default address
  public String deviceAddress = "0x40";
  /**
   * This address is to address all Adafruit16CServoDrivers on the i2c bus Don't
   * use this address for any other device on the i2c bus since it will cause
   * collisions.
   */
  public String broadcastAddress = "0x70";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class);

  public static final int PCA9685_MODE1 = 0x00; // Mod
  // 1
  // register
  public static final byte PCA9685_SLEEP = 0x10; // Set sleep mode before
  // changing prescale value
  public static final byte PCA9685_AUTOINCREMENT = 0x20; // Set autoincrement to
  // be able to write
  // more than one byte
  // in sequence

  public static final byte PCA9685_PRESCALE = (byte) 0xFE; // PreScale register

  // Pin PWM addresses 4 bytes repeats for each pin so I only define pin 0
  // The rest of the addresses are calculated based on pin numbers
  public static final int PCA9685_LED0_ON_L = 0x06; // First LED address Low
  public static final int PCA9685_LED0_ON_H = 0x07; // First LED address High
  public static final int PCA9685_LED0_OFF_L = 0x08; // First LED address Low
  public static final int PCA9685_LED0_OFF_H = 0x09; // First LED addressHigh

  public static final int PCA9685_ALL_LED_OFF_H = 0xFD; // All call i2c address
  // ( Used for shutdown
  // of all pwm )
  public static final int PCA9685_TURN_ALL_LED_OFF = 0x10; // Command to turn
  // all LED off stop
  // pwm )

  // public static final int PWM_FREQ = 60; // default frequency for servos
  public static final float osc_clock = 25000000; // clock frequency of the
  // internal clock
  public static final float precision = 4096; // pwm_precision

  /**
   * i2c controller
   */
  public List<String> controllers;

  /**
   * current i2c controllers name
   */
  public String controllerName;

  // isAttached is used by the GUI's to know it the service is attached or not
  // It will be set when the first successful communication has been done with
  // the
  // i2c device ( bus and address have been verified )
  @Deprecated /*
               * need to be explicit - isAttached in this context means
               * controller - servos or other devices could be attached as well
               */
  public boolean isAttached = false;

  /**
   * @author Mats - added by GroG - was wondering if this would help, probably
   *         you need a reverse index too ?
   * @author GroG - I only need servoNameToPin yet. To be able to move at a set
   *         speed a few extra values are needed
   */

  class ServoEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    String pin;
    SpeedControl speedcontrol;
    /**
     * velocity/speed - when its null - no additional speed control is used
     */
    Double velocity = null;
    double acceleration = -1;
    boolean isMoving = false;
    double targetOutput;
    double currentOutput;
    boolean isEnergized = false;
    ServoControl servo;
  }

  transient HashMap<String, ServoEvent> servoMap = new HashMap<String, ServoEvent>();

  // Motor related constants
  public static final int MOTOR_FORWARD = 1;
  public static final int MOTOR_BACKWARD = 0;
  public static final int defaultMotorPwmFreq = 1000;

  /**
   * pin named map of all the pins on the board
   */
  Map<String, PinDefinition> pinMap = null;
  /**
   * the definitive sequence of pins - "true address"
   */
  Map<Integer, PinDefinition> pinIndex = null;

  public static void main(String[] args) {

    LoggingFactory.init();

    Adafruit16CServoDriver driver = (Adafruit16CServoDriver) Runtime.start("pwm", "Adafruit16CServoDriver");
    log.info("Driver {}", driver);

  }

  public Adafruit16CServoDriver(String n, String id) {
    super(n, id);
    createPinList();
    registerForInterfaceChange(I2CController.class);
  }

  /**
   * @return Refresh the list of running services that can be selected in the
   *         GUI
   */
  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
    return controllers;
  }

  /**
   * function to convert labeled pins into address locations pin examples would
   * be D5, D6, ... on an Arduino or some other string value
   * 
   * @param pin
   *          the string representation of the pin
   * @return the integer representation of the pin address
   */
  public int getAddress(String pin) {
    try {
      return Integer.parseInt(pin);
    } catch (Exception e) {
      log.error("could not convert pin labeled as {} to an address", pin);
    }
    return 0;
  }

  /**
   * type conversion for setting the pwm
   * 
   * @param pinAddress
   *          the pin address
   * @param pulseWidthOn
   *          the on width
   * @param pulseWidthOff
   *          the off width
   * 
   */
  public void setPWM(Integer pinAddress, Integer pulseWidthOn, Integer pulseWidthOff) {
    setPWM(pinAddress + "", pulseWidthOn, pulseWidthOff);
  }

  /**
   * Set the pwm pulsewidth
   * 
   * @param pinLabel
   *          a label for the pin
   * @param pulseWidthOn
   *          the on pulse width
   * @param pulseWidthOff
   *          the off pulse width
   */
  public void setPWM(String pinLabel, Integer pulseWidthOn, Integer pulseWidthOff) {
    // TODO - handle pin label mappings if necessary
    int pin = getAddress(pinLabel);

    byte[] buffer = { (byte) (PCA9685_LED0_ON_L + (pin * 4)), (byte) (pulseWidthOn & 0xff), (byte) (pulseWidthOn >> 8), (byte) (pulseWidthOff & 0xff),
        (byte) (pulseWidthOff >> 8) };
    log.debug("Writing pin {}, pulesWidthOn {}, pulseWidthOff {}", pin, pulseWidthOn, pulseWidthOff);
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
  }

  /**
   * type conversion to set the pwm frequency
   * 
   * @param pin
   *          the pin
   * @param hz
   *          the frequency
   * 
   */
  public void setPWMFreq(Integer pin, Integer hz) {
    setPWMFreq(pin + "", hz);
  }

  /**
   * Set the PWM frequency. i.e. the frequency between positive pulses.
   * 
   * @param pin
   *          the pin. This is not really used as the frequency is set for all
   *          16 channels.
   * @param hz
   *          the frequency in hz
   * 
   */
  public void setPWMFreq(String pin, Integer hz) { // Analog servos run at ~60
                                                   // Hz

    float prescale_value;

    if (hz < minPwmFreq) {
      log.error("minimum pwm freq is {} hz, requested freqency is {} hz, clamping to minimum", minPwmFreq, hz);
      hz = minPwmFreq;
      prescale_value = 255;
    } else if (hz > maxPwmFreq) {
      log.error("maximum pwm freq is {} hz, requested frequency is {} hz, clamping to maximum", maxPwmFreq, hz);
      hz = maxPwmFreq;
      prescale_value = 3;
    } else {
      // Multiplying with factor 0.9 to correct the frequency
      // See
      // https://github.com/adafruit/Adafruit-PWM-Servo-Driver-Library/issues/11
      prescale_value = Math.round(0.9 * osc_clock / precision / hz) - 1;
    }

    log.info("pwm frequency {} hz, prescale_value calculated to %s", hz, prescale_value);
    // Set sleep mode before changing PWM freqency
    byte[] writeBuffer = { PCA9685_MODE1, PCA9685_SLEEP };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writeBuffer, writeBuffer.length);

    // Wait 1 millisecond until the oscillator has stabilized
    sleep(1);

    // Write the PWM frequency value
    byte[] buffer2 = { PCA9685_PRESCALE, (byte) prescale_value };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer2, buffer2.length);

    // Leave sleep mode, set autoincrement to be able to write several
    // bytes
    // in sequence
    byte[] buffer3 = { PCA9685_MODE1, PCA9685_AUTOINCREMENT };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer3, buffer3.length);

    // Wait 1 millisecond until the oscillator has stabilized
    sleep(1);

    pwmFreq = hz;
    pwmFreqSet = true;

  }

  /**
   * Orderly shutdown. Send a message to stop all pwm generation
   */
  public void stopPwm() {
    if (controller == null) {
      return;
    }

    byte[] buffer = { (byte) (PCA9685_ALL_LED_OFF_H), (byte) PCA9685_TURN_ALL_LED_OFF };
    log.info("Writing shutdown command to {}", this.getName());
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
  }

  void setServo(int pin, Integer pulseWidthOff) {
    setServo(pin + "", pulseWidthOff);
  }

  void setServo(String pin, Integer pulseWidthOff) {
    // since pulseWidthOff can be larger than > 256 it needs to be
    // sent as 2 bytes
    setPWM(pin, 0, pulseWidthOff);
  }

  @Override
  public void onServoMoveTo(ServoMove move) {
    ServoControl servo = (ServoControl) Runtime.getService(move.name);
    ServoEvent ServoEvent = servoMap.get(servo.getName());
    if (!pwmFreqSet) {
      setPWMFreq(ServoEvent.pin, defaultPwmFreq);
    }

    if (ServoEvent.isEnergized) {
      // Move at max speed
      if (ServoEvent.velocity == null || ServoEvent.velocity == -1) {
        log.debug("Ada move at max speed");
        ServoEvent.currentOutput = servo.getTargetOutput();
        ServoEvent.targetOutput = servo.getTargetOutput();
        log.debug("servoWrite {} deviceAddress {} targetOutput {}", servo.getName(), deviceAddress, servo.getTargetOutput());
        int pulseWidthOff = SERVOMIN + (int) (servo.getTargetOutput() * (int) ((float) SERVOMAX - (float) SERVOMIN) / (180));
        setServo(servo.getPin(), pulseWidthOff);
        publishServoEvent(ServoEvent.servo, 1, ServoEvent.targetOutput);
      } else {
        log.debug("Ada move at velocity {} degrees/s", ServoEvent.velocity);
        ServoEvent.targetOutput = servo.getTargetOutput();
        // Start a thread to handle the speed for this servo
        if (ServoEvent.isMoving == false) {
          ServoEvent.speedcontrol = new SpeedControl(servo.getName());
          ServoEvent.speedcontrol.start();
        }
      }
    }
  }

  /**
   * Set the servo to a position using uS
   * 
   * @param servo
   *          Servo service to be moved
   * @param uS
   *          A value between 500 and 2500
   * 
   */
  @Override
  public void onServoWriteMicroseconds(ServoControl servo, int uS) {
    ServoEvent ServoEvent = servoMap.get(servo.getName());
    if (!pwmFreqSet) {
      setPWMFreq(ServoEvent.pin, defaultPwmFreq);
    }

    int pin = getAddress(servo.getPin());
    // 1000 ms => 150, 2000 ms => 600
    int pulseWidthOff = (int) (uS * 0.45) - 300;
    // since pulseWidthOff can be larger than > 256 it needs to be
    // sent as 2 bytes
    log.debug("servoWriteMicroseconds {} deviceAddress {} pin {} pulse {}", servo.getName(), deviceAddress, pin, pulseWidthOff);

    byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (pin * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
  }

  public String publishAttachedDevice(String deviceName) {
    return deviceName;
  }

  public void servoSetMaxVelocity(ServoControl servo) {
    log.warn("servoSetMaxVelocity not implemented in Adafruit16CServoDriver");

  }

  @Override
  public void motorMove(MotorControl mc) {

    Class<?> type = mc.getClass();

    // FIXME - do not count on MotorControl's MotorControl's getPowerOutput
    // to produce the correct values for MotorController
    // double powerOutput = mc.getPowerOutput();

    // this is guaranteed to be between -1.0 and 1.0
    // double powerLevel = mc.getPowerLevel();

    double powerLevel = mc.getPowerLevel();
    if (mc.isInverted()) {
      powerLevel = powerLevel * -1;
    }

    if (Motor.class == type) {
      Motor motor = (Motor) mc;
      if (motor.getPwmFreq() == null) {
        motor.setPwmFreq(defaultMotorPwmFreq);
        setPWMFreq(motor.getPwrPin(), motor.getPwmFreq());
      }
      setPinValue(motor.getDirPin(), (powerLevel < 0) ? MOTOR_BACKWARD : MOTOR_FORWARD);
      setPinValue(motor.getPwrPin(), powerLevel);
    } else if (MotorDualPwm.class == type) {
      MotorDualPwm motor = (MotorDualPwm) mc;
      log.info("Adafruit16C Motor DualPwm motorMove, powerLevel = {}", powerLevel);
      if (motor.getPwmFreq() == null) {
        motor.setPwmFreq(defaultMotorPwmFreq);
        setPWMFreq(motor.getLeftPwmPin(), motor.getPwmFreq());
        setPWMFreq(motor.getRightPwmPin(), motor.getPwmFreq());
      }
      if (powerLevel < 0) {
        setPinValue(motor.getLeftPwmPin(), 0);
        setPinValue(motor.getRightPwmPin(), Math.abs(powerLevel));
      } else if (powerLevel > 0) {
        setPinValue(motor.getRightPwmPin(), 0);
        setPinValue(motor.getLeftPwmPin(), Math.abs(powerLevel));
      } else {
        setPinValue(motor.getRightPwmPin(), 0);
        setPinValue(motor.getLeftPwmPin(), 0);
      }
    } else {
      error("motorMove for motor type %s not supported", type);
    }
  }

  @Override
  public void motorMoveTo(MotorControl mc) {
    // speed parameter?
    // modulo - if < 1
    // speed = 1 else
    log.info("motorMoveTo targetPos {} powerLevel {}", mc.getTargetPos(), mc.getPowerLevel());

    Class<?> type = mc.getClass();

    // if pulser (with or without fake encoder
    // send a series of pulses !
    // with current direction
    if (Motor.class == type) {
      Motor motor = (Motor) mc;
      // check motor direction
      // send motor direction
      // TODO powerLevel = 100 * powerlevel

      // FIXME !!! - this will have to send a Long for targetPos at some
      // point !!!!
      double target = Math.abs(motor.getTargetPos());
      /*
       * int b0 = (int) target & 0xff; int b1 = ((int) target >> 8) & 0xff; int
       * b2 = ((int) target >> 16) & 0xff; int b3 = ((int) target >> 24) & 0xff;
       */

      // TODO FIXME
      // sendMsg(PULSE, deviceList.get(motor.getName()).id, b3, b2, b1,
      // b0, (int) motor.getPowerLevel(), feedbackRate);
    }

  }

  @Override
  public void motorStop(MotorControl mc) {

    Class<?> type = mc.getClass();

    if (Motor.class == type) {
      Motor motor = (Motor) mc;
      if (motor.getPwmFreq() == null) {
        motor.setPwmFreq(defaultMotorPwmFreq);
        setPWMFreq(motor.getPwrPin(), motor.getPwmFreq());
      }
      setPinValue(motor.getPwrPin(), 0);
    } else if (MotorDualPwm.class == type) {
      MotorDualPwm motor = (MotorDualPwm) mc;
      setPinValue(motor.getLeftPwmPin(), 0);
      setPinValue(motor.getRightPwmPin(), 0);
    }

  }

  @Override
  public void motorReset(MotorControl motor) {
    // perhaps this should be in the motor control
    // motor.reset();
    // opportunity to reset variables on the controller
    // sendMsg(MOTOR_RESET, motor.getind);

  }

  /**
   * Set Output Power of a pin. This is a value betweeom 0.0 and 1.0 if you are
   * using this to drive an LED. Connect the LED between VCC and the output pic,
   * then invert this value where 1.0 is off and 0.0 is full on.
   * 
   * @param pinLabel
   *          the pin not really used
   * @param powerOutput
   *          A value between 0.0 and 1.0
   * 
   */
  public void setPinValue(String pinLabel, double powerOutput) {
    int pin = getAddress(pinLabel);
    log.info("Adafruit16C setPinValue, pin = {}, powerOutput = {}", pin, powerOutput);
    if (powerOutput < 0) {
      log.error("Adafruit16CServoDriver setPinValue. Value below zero ({}). Defaulting to 0.", powerOutput);
      powerOutput = 0;
    } else if (powerOutput > 1) {
      log.error("Adafruit16CServoDriver setPinValue. Value > 1 ({}). Defaulting to 1", powerOutput);
      powerOutput = 1;
    }

    int powerOn;
    int powerOff;
    // No phase shift. Simple calculation
    if (powerOutput == 0) {
      powerOn = 0;
      powerOff = 0;
    } else if (powerOutput == 1) {
      powerOn = 0;
      powerOff = 4095;
    } else {
      powerOn = 0;
      powerOff = (int) (powerOutput * 4096);
    }
    log.info("powerOutput = {}, powerOn = {}, powerOff = {}", powerOutput, powerOn, powerOff);
    setPWM(pin, powerOn, powerOff);
  }

  public Map<String, PinDefinition> createPinList() {
    pinIndex = new HashMap<Integer, PinDefinition>();

    for (int i = 0; i < 16; ++i) {
      PinDefinition pindef = new PinDefinition(getName(), i, String.format("D%d", i));
      pindef.setDigital(true);
      pinIndex.put(i, pindef);
    }
    return pinMap;
  }

  @Override
  public void onServoSetSpeed(ServoSpeed servoSpeed) {
    ServoEvent ServoEvent = servoMap.get(servoSpeed.name);
    ServoEvent.velocity = servoSpeed.speed;
  }

  public List<PinDefinition> getPinList() {
    List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
    pinMap = new TreeMap<String, PinDefinition>();
    pinIndex = new TreeMap<Integer, PinDefinition>();
    List<PinDefinition> pinList = new ArrayList<PinDefinition>();

    for (int i = 0; i < 15; ++i) {

      PinDefinition pindef = new PinDefinition(getName(), i, String.format("D%d", i));
      pindef.setRx(false);
      pindef.setDigital(true);
      pindef.setAnalog(true);
      pindef.setDigital(true);
      pindef.canWrite(true);
      pindef.setPwm(true);
      pindef.setAddress(i);
      pinIndex.put(i, pindef);
      pinMap.put(pindef.getPinName(), pindef);
      pinList.add(pindef);
    }
    return list;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    // FIXME - this is messy !
    if (controller == null) {
      return false;
    }

    // attached controller
    if (controller != null && controller == instance) {
      return isAttached;
    }

    // attached servo
    return servoMap.containsKey(instance.getName());
  }

  @Override
  public void stopService() {
    if (!isAttached(controller)) {
      detachI2CController(controller);
    }
    super.stopService(); // stop inbox and outbox
  }

  @Override
  public void setDeviceBus(String deviceBus) {
    if (isAttached) {
      log.error("Already attached to {}, use detach({}) first", this.controllerName);
      return;
    }
    this.deviceBus = deviceBus;
    broadcastState();
  }

  @Override
  public void setDeviceAddress(String deviceAddress) {
    if (isAttached) {
      log.error("Already attached to {}, use detach({}) first", this.controllerName);
      return;
    }
    this.deviceAddress = deviceAddress;
    broadcastState();
  }

  @Deprecated /* use attach */
  public void setController(String controllerName, String deviceBus, String deviceAddress) {
    attach(controllerName, deviceBus, deviceAddress);
  }

  @Deprecated // use attach(I2CController controller)
  public void setController(I2CController controller) {
    try {
      attach(controller);
    } catch (Exception e) {
      log.error("setController / attach throw", e);
    }
  }

  @Deprecated // use attach(I2CController controller)
  public void setController(I2CController controller, String deviceBus, String deviceAddress) {
    this.deviceBus = deviceBus;
    this.deviceAddress = deviceAddress;
    attach(controller);
  }

  @Override /* route depending on interface */
  public void attach(Attachable service) {

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      attachI2CController((I2CController) service);
      return;
    }

    if (ServoControl.class.isAssignableFrom(service.getClass())) {
      attachServoControl((ServoControl) service);
      return;
    }
  }

  public void attach(String controllerName, String deviceBus, String deviceAddress) {
    attach((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
  }

  @Override
  public void attach(I2CController controller, String deviceBus, String deviceAddress) {
    this.deviceBus = deviceBus;
    this.deviceAddress = deviceAddress;
    attachI2CController(controller);
  }

  @Override
  public void attachI2CController(I2CController controller) {

    // FIXME - way way too complex - clean up
    if (isAttached(controller))
      return;

    if (this.controllerName != null && this.controllerName != controller.getName()) {
      log.info("Trying to attached to {}, but already attached to ({})", controller.getName(), this.controllerName);
      return;
    }

    if (isAttached && this.controller != controller) {
      log.error("Already attached to {}, use detach({}) first", this.controllerName, controller.getName());
    }

    controllerName = controller.getName();
    log.info("{} attach {}", getName(), controllerName);

    this.controller = controller;
    isAttached = true;
    controller.attachI2CControl(this);
    log.info("Attached {} device on bus: {} address {}", controllerName, deviceBus, deviceAddress);
    broadcastState();
  }

  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    servo.setPin(pin);
    attachServoControl(servo);
  }

  @Override
  public void attachServoControl(ServoControl servo) {
    if (isAttachedServoControl(servo)) {
      log.info("servo {} already attached", servo.getName());
      return;
    }
    ServoEvent ServoEvent = new ServoEvent();
    ServoEvent.pin = servo.getPin();
    ServoEvent.targetOutput = servo.getTargetOutput();
    ServoEvent.currentOutput = servo.getTargetOutput();
    ServoEvent.velocity = servo.getSpeed();
    ServoEvent.isEnergized = true;
    ServoEvent.servo = servo;
    servoMap.put(servo.getName(), ServoEvent);
    servo.attach(this);
  }

  @Override
  public void detach() {
    if (controllerName != null) {
      detach(Runtime.getService(controllerName));
    }
    broadcastState();
  }

  // This section contains all the new detach logic
  // TODO: This default code could be in Attachable
  @Override
  public void detach(String service) {
    detach(Runtime.getService(service));
  }

  @Override
  public void detach(Attachable service) {
    if (!isAttached(service)) { // we're done
      return;
    }

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      detachI2CController((I2CController) service);
    }

    if (ServoControl.class.isAssignableFrom(service.getClass())) {
      try {
        // detachServoControl already call servoDetachPin
        // servoDetachPin((ServoControl) service);
        if (service.isAttached(this)) {
          detachServoControl((ServoControl) service);
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block);
        log.error("setController / attach throw", e);
      }
      return;
    }
  }

  @Override
  public void detachI2CController(I2CController controller) {
    log.info("stopping pwm");
    stopPwm();
    log.info("isAttached = false");
    isAttached = false;
    if (controllerName == null) {
      log.info("already detached");
      return;
    }
    // should be by name - not by 'this' reference
    log.info("removing controller name");
    controllerName = null;
    log.info("requesting detach from i2c controller");
    controller.detachI2CControl(this);
    broadcastState();
  }

  public void detachServoControl(ServoControl servo) throws Exception {
    if (servoMap.containsKey(servo.getName())) {
      servoMap.remove(servo.getName());
      servo.detach(this);
    }
  }

  // This section contains all the methods used to query / show all attached
  // services
  public boolean isAttachedServoControl(ServoControl servo) {
    return servoMap.containsKey(servo.getName());
  }

  /**
   * Returns all the currently attached services TODO Add the list of attached
   * motors
   */
  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null && isAttached) {
      ret.add(controller.getName());
    }
    ret.addAll(servoMap.keySet());
    return ret;
  }

  @Override
  public String getDeviceBus() {
    return this.deviceBus;
  }

  @Override
  public String getDeviceAddress() {
    return this.deviceAddress;
  }

  @Override
  public void onServoEnable(String servoName) {
    ServoEvent ServoEvent = servoMap.get(servoName);
    if (ServoEvent == null) {
      log.error("servo data {} could not get servo from map", servoName);
      return;
    }
    int pulseWidthOff = SERVOMIN + (int) (ServoEvent.servo.getTargetOutput() * (int) ((float) SERVOMAX - (float) SERVOMIN) / (180));
    setPWM(ServoEvent.pin, 0, pulseWidthOff);
    ServoEvent.isEnergized = true;
    log.info("pin " + ServoEvent.pin + " enabled from " + servoName);
  }

  @Override
  public void onServoDisable(String servoName) {
    ServoEvent ServoEvent = servoMap.get(servoName);
    if (ServoEvent == null) {
      log.error("servo data {} could not get servo from map", servoName);
      return;
    }
    setPWM(ServoEvent.pin, 0, 0);
    ServoEvent.isEnergized = false;
    log.info("pin " + ServoEvent.pin + " disabled from " + servoName);
  }

  // currently not a "real" motor control - it has to wait for merging of Servo
  // & Motor
  @Override
  public List<String> getPorts() {
    // we use pins not ports
    List<String> ret = new ArrayList<String>();
    return ret;
  }

  // currently not a "real" motor control - it has to wait for merging of Servo
  // & Motor
  @Override
  public Mapper getDefaultMapper() {
    // best guess :P
    MapperLinear mapper = new MapperLinear();
    mapper.map(-1.0, 1.0, 0.0, 255.0);
    return mapper;
  }

  // not used currently - should be refactored to use these methods for motor
  // control
  @Override
  public double motorCalcOutput(MotorControl mc) {
    double value = mc.calcControllerOutput();
    return value;
  }

  // FIXME - implement - if there is speed control
  // "stopping" should stop the servo
  @Override
  public void onServoStop(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Deprecated /*
               * controllers shouldn't publish "servo events" - they should
               * broacast encoder data back to a ServoControl
               */
  // @Override
  public String publishServoStarted(String name) {
    return name;
  }

  @Deprecated /*
               * controllers shouldn't publish "servo events" - they should
               * broacast encoder data back to a ServoControl
               */
  // @Override
  public String publishServoStopped(String name) {
    return name;
  }
  
  @Override
  public Adafruit16CServoDriverConfig getConfig() {
    super.getConfig();
    Adafruit16CServoDriverConfig config = (Adafruit16CServoDriverConfig)super.getConfig();
    // FIXME remove member vars use config directly
    config.controller = controllerName;
    config.deviceBus = deviceBus;
    config.deviceAddress = deviceAddress;
    return config;
  }
  
  @Override
  public Adafruit16CServoDriverConfig apply(Adafruit16CServoDriverConfig c) {
    super.apply(c);
    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch (Exception e) {
        log.error("attaching controller failed", e);
      }
    }
    
    // lame - this shouldn't be "copied" over - everything should just simply use config.deviceAddress
    if (config.deviceAddress != null) {
      deviceAddress = config.deviceAddress;
    }

    if (config.deviceBus != null) {
      deviceBus = config.deviceBus;
    }
    
    return c;
  }

  @Override
  public void setBus(String bus) {
    setDeviceBus(bus);
  }

  @Override
  public void setAddress(String address) {
    setDeviceAddress(address);
  }

  @Override
  public String getBus() {
    return deviceBus;
  }

  @Override
  public String getAddress() {
    return deviceAddress;
  }

}