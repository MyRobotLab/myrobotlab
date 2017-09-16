/*
 * 
 *   AdafruitMotorShield
 *   
 *   TODO - test with Steppers &amp; Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.slf4j.Logger;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author GroG
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 * 
 *         FIXME - re-write with new MRLComm device - DO NOT USE ANCILLARY
 *         LIBRARIES LIKE AF_MOTOR !!!
 */

public class AdafruitMotorShield extends Service implements MotorController, ArduinoShield {
  /** version of the library */
  static public final String VERSION = "0.9";

  private static final long serialVersionUID = 1L;

  /*
   * TODO - make step calls NON BLOCKING 1. make step calls non-blocking - they
   * don't block MRL - but they block the processing of the Arduino which is not
   * needed (or desired) 2. nice SwingGui for steppers 3. release functionality
   * 4. style set <-- easy
   */

  // AF Shield controls these 2 servos
  // makes "attaching" impossible
  // Servo servo10;

  final public int FORWARD = 1;
  final public int BACKWARD = 2;
  final public int BRAKE = 3;
  final public int RELEASE = 4;

  final public int SINGLE = 1;
  final public int DOUBLE = 2;
  final public int INTERLEAVE = 3;
  final public int MICROSTEP = 4;

  HashMap<String, Integer> deviceNameToNumber = new HashMap<String, Integer>();

  transient public HashMap<String, Motor> motors = new HashMap<String, Motor>();
  transient public HashMap<String, Servo> servos = new HashMap<String, Servo>();

  transient public Arduino arduino = null;

  final int AF_DCMOTOR_ATTACH = 51;
  final int AF_DCMOTOR_DETACH = 52;
  final int AF_DCMOTOR_RELEASE = 53;
  final int AF_DCMOTOR_SET_SPEED = 54;
  final int AF_DCMOTOR_RUN_COMMAND = 55;

  // FIXME - COMPLETE IMPLEMENTATION BEGIN --
  final int AF_STEPPER_ATTACH = 56;
  final int AF_STEPPER_DETACH = 57; // release
  final int AF_STEPPER_RELEASE = 58; // release
  final int AF_STEPPER_STEP = 59;
  final int AF_STEPPER_SET_SPEED = 60;
  // FIXME - COMPLETE IMPLEMENTATION END --

  public static final String ADAFRUIT_DEFINES =

      "\n\n" + "#include <AFMotor.h>\n\n" + " #define AF_DCMOTOR_ATTACH 51\n" + " #define AF_DCMOTOR_DETACH 52\n" + " #define AF_DCMOTOR_RELEASE 53\n"
          + " #define AF_DCMOTOR_SET_SPEED 54\n" + " #define AF_DCMOTOR_RUN_COMMAND 55\n" + "\n" + " #define AF_STEPPER_ATTACH 56\n" + " #define AF_STEPPER_DETACH 57\n"
          + " #define AF_STEPPER_RELEASE 58\n" + " #define AF_STEPPER_STEP 59\n" + " #define AF_STEPPER_SET_SPEED 60\n" +

          " AF_DCMotor* motorMap[4];\n" + " AF_Stepper* stepperMap[2];\n" + "\n\n";

  public int direction = FORWARD;

  // the last amount (abs) step command
  // private int step = 0;

  public transient final static Logger log = LoggerFactory.getLogger(AdafruitMotorShield.class.getCanonicalName());

  public static final String ADAFRUIT_SETUP = "";

  public static final String ADAFRUIT_CODE = "\n\n" + "            case AF_DCMOTOR_ATTACH:{ \n" + "              motorMap[ioCommand[1] - 1] =  new AF_DCMotor(ioCommand[1]);\n "
      + "            }\n" + "            break; \n" +

      "            case AF_DCMOTOR_DETACH:{ \n" + "             motorMap[ioCommand[2]]->run(RELEASE); \n" + "             delete motorMap[ioCommand[2]];\n" + "            }\n"
      + "            break; \n" +

      "            case AF_DCMOTOR_RELEASE:{ \n" + "             motorMap[ioCommand[2]]->run(RELEASE); \n" + "            }\n" + "            break; \n" +

      "            case AF_DCMOTOR_RUN_COMMAND:{ \n" + "             motorMap[ioCommand[1]]->run(ioCommand[2]); \n" + "            }\n" + "            break; \n" +

      "            case AF_DCMOTOR_SET_SPEED:{ \n" + "             motorMap[ioCommand[1]]->setSpeed(ioCommand[2]); \n" + "            }\n" + "            break; \n" +

      "\n\n" + "            case AF_STEPPER_ATTACH:{ \n" + "              stepperMap[ioCommand[2] - 1] = new AF_Stepper (ioCommand[1], ioCommand[2]);\n " + "            }\n"
      + "            break; \n" +

      "            case AF_STEPPER_DETACH:{ \n" + "             stepperMap[ioCommand[1]-1]->release(); \n" + "             delete stepperMap[ioCommand[1]-1]; \n"
      + "            }\n" + "            break; \n" +

      "            case AF_STEPPER_RELEASE:{ \n" + "             stepperMap[ioCommand[1]-1]->release(); \n" + "            }\n" + "            break; \n" +

      "            case AF_STEPPER_STEP:{ \n" + "             stepperMap[ioCommand[1]-1]->step(((ioCommand[2] << 8) + ioCommand[3]), ioCommand[4], ioCommand[5]); \n"
      + "            }\n" + "            break; \n" +

      "            case AF_STEPPER_SET_SPEED:{ \n" + "             stepperMap[ioCommand[1]-1]->setSpeed(ioCommand[2]); \n" + "            }\n" + "            break; \n";

  public static void main(String[] args) {

    LoggingFactory.init(Level.DEBUG);

    try {
      // FIXME !!! - don't use Adafruit's library - do your own stepper control
      // through "pure" MRLComm.ino
      AdafruitMotorShield fruity = (AdafruitMotorShield) Runtime.createAndStart("fruity", "AdafruitMotorShield");
      Runtime.createAndStart("gui01", "SwingGui");

      fruity.connect("COM3");

      Motor motor1 = fruity.createDCMotor(4);
      motor1.move(0.4f);

      // create a 200 step stepper on adafruitsheild port 1
      // Stepper stepper1 = fruity.createStepper(200, 1);

      // FIXME - needs to be cleaned up - tear down
      // fruity.releaseStepper(stepper1.getName());

      // Runtime.createAndStart("python", "Python");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public AdafruitMotorShield(String n) {
    super(n);
    arduino = (Arduino) createPeer("arduino");
  }

  /**
   * an Arduino does not need to know about a shield but a shield must know
   * about a Arduino Arduino owns the script, but a Shield needs additional
   * support Shields are specific - but plug into a generalized Arduino Arduino
   * shields can not be plugged into other uCs
   * 
   * TODO - Program Version &amp; Type injection - with feedback + query to load
   */
  @Override
  public boolean attach(Arduino inArduino) {

    if (inArduino == null) {
      error("can't attach - arduino is invalid");
      return false;
    }

    this.arduino = inArduino;

    // arduinoName; FIXME - get clear on diction Program Script or Sketch
    StringBuffer newProgram = new StringBuffer();
    newProgram.append(arduino.getSketch());

    /*
     * 
     * // modify the program int insertPoint =
     * newProgram.indexOf(Arduino.VENDOR_DEFINES_BEGIN);
     * 
     * if (insertPoint > 0) {
     * newProgram.insert(Arduino.VENDOR_DEFINES_BEGIN.length() + insertPoint,
     * ADAFRUIT_DEFINES); } else { error(
     * "could not find insert point in MRLComm.ino"); // get info back to user
     * return false; }
     * 
     * insertPoint = newProgram.indexOf(Arduino.VENDOR_SETUP_BEGIN);
     * 
     * if (insertPoint > 0) {
     * newProgram.insert(Arduino.VENDOR_SETUP_BEGIN.length() + insertPoint,
     * ADAFRUIT_SETUP); } else { error(
     * "could not find insert point in MRLComm.ino"); // get info back to user
     * return false; }
     * 
     * insertPoint = newProgram.indexOf(Arduino.VENDOR_CODE_BEGIN);
     * 
     * if (insertPoint > 0) {
     * newProgram.insert(Arduino.VENDOR_CODE_BEGIN.length() + insertPoint,
     * ADAFRUIT_CODE); } else { error(
     * "could not find insert point in MRLComm.ino"); // get info back to user
     * return false; }
     */

    // set the program
    Sketch sketch = new Sketch("AdafruitMotorShield", newProgram.toString());
    arduino.setSketch(sketch);
    // broadcast the arduino state - ArduinoGUI should subscribe to
    // setProgram
    broadcastState(); // state has changed let everyone know

    // servo9.attach(arduinoName, 9); // FIXME ??? - createServo(Integer i)
    // servo10.attach(arduinoName, 10);

    // error(String.format("couldn't find %s", arduinoName));
    return true;
  }

  public void connect(String port) throws IOException {
    arduino.connect(port);
  }

  public void connect(String port, Integer rate, int databits, int stopbit, int parity) throws IOException {
    arduino.connect(port, rate, databits, stopbit, parity);
  }

  /**
   * creates a DC Motor on port 1,2,3, or 4
   * @param motorNum num
   * @return a motor
   * @throws Exception e
   * 
   */
  public Motor createDCMotor(Integer motorNum) throws Exception {
    if (motorNum == null || motorNum < 1 || motorNum > 4) {
      error(String.format("motor number should be 1,2,3,4 not %d", motorNum));
      return null;
    }
    String motorName = String.format("%s_m%d", getName(), motorNum);
    deviceNameToNumber.put(motorName, motorNum);
    Motor m = new Motor(motorName);
    m.startService();
    motors.put(motorName, m);
    m.broadcastState();
    m.attachMotorController(this);
    return m;
  }

  /**
   * creates a stepper on stepper port 1 or 2
   * @param steps s
   * @param stepperPort port 
   * @return a motor or null
   */
  public Motor createStepper(Integer steps, Integer stepperPort) {
    if (stepperPort == null || stepperPort < 1 || stepperPort > 2) {
      error(String.format("stepper number should 1 or 2 not %d", stepperPort));
      return null;
    }

    String stepperName = String.format("%s_stepper%d", getName(), stepperPort);
    log.debug("Stepper name: {}", stepperName);
    /*
     * if (steppers.containsKey(stepperName)) { warn("%s alreaady exists",
     * stepperName); return steppers.get(stepperName); }
     */

    return null;
  }

  @Override
  public boolean isAttached() {
    return arduino != null;
  }

  // MotorController end ----
  // StepperController begin ----

  public void setSpeed(Integer motorPortNumber, Integer speed) {
    // TODO - fix
    // arduino.sendMsg(new MrlMsg(AF_DCMOTOR_SET_SPEED).append(motorPortNumber -
    // 1).append(speed));
  }

  // VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
  // DC Motors
  // ----------- AFMotor API Begin --------------
  public void setSpeed(String name, Integer speed) { // FIXME - sloppy
    setSpeed(deviceNameToNumber.get(name) - 1, speed);
  }

  @Override
  public void startService() {
    super.startService();
    arduino.startService();
    attach(arduino);
  }

  // Stepper Motors
  public void step(int count, int direction, int type) {

  }

  // StepperController end ----

  @Override
  public void motorMove(MotorControl motor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void motorMoveTo(MotorControl motor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void motorStop(MotorControl motor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void motorReset(MotorControl motor) {
    // TODO Auto-generated method stub

  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(AdafruitMotorShield.class.getCanonicalName());
    meta.addDescription("arduino shield which controls 4 dc motors or 2 steppers");
    meta.setLicenseApache();
    
    meta.addCategory("shield", "motor");
    meta.addPeer("arduino", "Arduino", "our Arduino");
    return meta;
  }

  @Override
  public Set<String> getAttached() {
    return motors.keySet();
  }
}