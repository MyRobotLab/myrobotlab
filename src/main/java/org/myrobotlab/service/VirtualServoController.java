package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

public class VirtualServoController extends Service implements ServoController {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VirtualServoController.class);

  // FIXME - use interface Simulator
  transient JMonkeyEngine jme;

  // FIXME - make more generalized "anything" could need speed control .. not
  // just Servos !
  Map<String, SpeedControl> speedControl = new HashMap<String, SpeedControl>();

  public VirtualServoController(String n) {
    super(n);
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

    ServiceType meta = new ServiceType(VirtualServoController.class);
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary

    // TEMPORARY CORE DEPENDENCIES !!! (for uber-jar)
    // meta.addDependency("orgId", "artifactId", "2.4.0");
    // meta.addDependency("org.bytedeco.javacpp-presets", "artoolkitplus",
    // "2.3.1-1.4");
    // meta.addDependency("org.bytedeco.javacpp-presets",
    // "artoolkitplus-platform", "2.3.1-1.4");

    // meta.addDependency("com.twelvemonkeys.common", "common-lang", "3.1.1");

    meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  // FIXME - make it an interface Simulator
  public void attach(JMonkeyEngine jme) throws Exception {

  }

  @Override
  public void attachServoControl(ServoControl servo) throws Exception {
    // FIXME - will it conflict with auto-attaching ?
    log.info("attaching servo control {}", servo.getName());

    // check to see we have a simulator reference

    // tell the simulator we are adding a servo
    // jme.attach(servo);

    // relay the attach to another ServoController if set (real ?)
  }

  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    servo.setPin(pin);
    attachServoControl(servo);
  }

  @Override
  public void servoAttachPin(ServoControl servo, Integer pin) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSweepStart(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSweepStop(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  // FIXME - chain another ServoController vs supporting multiple
  // ServoControllers ?
  public void chain(ServoController other) {

  }

  /**
   * 
   * 
   * https://relativity.net.au/gaming/java/Physics.html
   */
  class SpeedControl implements Runnable {
    VirtualServoController controller;
    ServoControl servo;
    boolean isRunning = false;
    transient Thread worker;
    // Double startPos;
    // Double targetPos;
    // Double velocity;
    long startTs;

    int eventsPerDegree = 6;

    SpeedControl(VirtualServoController controller, ServoControl servo) {
      this.controller = controller;
      this.servo = servo;

    }

    public void run() {

      // preserving servo vars from change since servo is a reference
      double velocity = servo.getVelocity();
      double startPos = servo.getCurrentPosOutput();
      double targetPos = servo.getPos();
      Double lastPos = servo.getLastPos();
      double totalDeltaPos = (targetPos - startPos);
      double totalEta = totalDeltaPos * velocity * 1000;
      log.info("moving {} from {} -to-> {} a total of {} degrees @ {} degrees/second for {} ms", servo.getName(), startPos, targetPos, totalDeltaPos, velocity, totalEta);

      // long t = Sys.getTime(); // LWJGL library.
      // System.nanoTime()
      // Thread.sleep(millis);
      isRunning = true;
      startTs = System.currentTimeMillis();
      long etaTs = startTs + (long) totalEta;
      log.info("move {} degrees in {} ms @ {} ts", (targetPos - startPos), totalEta, etaTs);

      double pos = 0; // our current position of the servo
      targetPos = targetPos * 1000; // now 1/1000 of a degree in 1 ms

      while (isRunning) {

        // our current now
        long now = System.currentTimeMillis();

        if (pos >= targetPos) { // if (now >= etaTs) { // change to pos ==
                                // targetPos
          log.info("{} reached {} target position eta {} - finished move", servo.getName(), pos, targetPos);
          isRunning = false;
          worker = null;
          return;
        }

        long deltaTime = now - startTs;
        // we have a current/lastPos and a -> target pos

        // find the new position based on deltaTime and the desired speed of the
        // servo
        pos = startPos + deltaTime * velocity;

        log.info("incremental move {} from {} to {} position at deta time {} ms is {}", servo.getName(), startPos, targetPos, deltaTime, pos / 1000);
        jme.rotateTo(servo.getName(), lastPos - (pos / 1000));
        // eventQueue.add()
        // servo.writeMicroseconds(uS);
        // Message msg = Message.createMessage(getName(), jme.getName(),
        // "servoMoveTo", new Object[] { servo });
        // all translation logic should be here from the the servo data to 3d
        // a simple command to rotate is needed (with name info)
        // jme.addMsg(msg);
        // jme.rotate();

        try {
          Thread.sleep(16); // 16ms roughly is 60 fps
        } catch (InterruptedException e) {
          isRunning = false;
        }
      }
    }

    // FIXME sychronize on worker object
    synchronized public void start() {
      if (worker == null) {
        worker = new Thread(this, String.format("%s-SpeedControl", servo.getName()));
        worker.start();
      }
    }

    synchronized public void stop() {
      if (worker != null) {
        worker.interrupt();
      }
    }

  }

  // FIXME - does invoking fix this ??? jme would selectively subscribe ??? or a
  // Simulator would selectively subscribe ???
  // to make
  @Override
  public void servoMoveTo(ServoControl servo) {

    SpeedControl sc = null;
    String name = servo.getName();
    if (!speedControl.containsKey(name)) {
      sc = new SpeedControl(this, servo);
      speedControl.put(name, sc);
    } else {
      sc = speedControl.get(name);
    }
    sc.start();
    /*
     * Message msg = Message.createMessage(getName(), jme.getName(),
     * "servoMoveTo", new Object[] { servo }); jme.addMsg(msg);
     */
    // jme.update
  }

  // FIXME !!! USER INTERFACE !!
  public void attachSimulator(JMonkeyEngine jme) {
    this.jme = jme;
  }

  @Override
  public void servoWriteMicroseconds(ServoControl servo, int uS) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoDetachPin(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSetVelocity(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSetAcceleration(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enablePin(Integer sensorPin, Integer i) {
    // TODO Auto-generated method stub

  }

  @Override
  public void disablePin(Integer i) {
    // TODO Auto-generated method stub

  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}