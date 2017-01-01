package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * InMoovHead - This is the inmoov head service. This service controls the
 * servos for the following: jaw, eyeX, eyeY, rothead and neck.
 * 
 */
public class InMoovHead extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovHead.class);

  transient public Servo jaw;
  transient public Servo eyeX;
  transient public Servo eyeY;
  transient public Servo rothead;
  transient public Servo neck;
  transient public Arduino arduino;

  public InMoovHead(String n) {
    super(n);
    jaw = (Servo) createPeer("jaw");
    eyeX = (Servo) createPeer("eyeX");
    eyeY = (Servo) createPeer("eyeY");
    rothead = (Servo) createPeer("rothead");
    neck = (Servo) createPeer("neck");
    arduino = (Arduino) createPeer("arduino");


    neck.setMinMax(20, 160);
    rothead.setMinMax(30, 150);
    // reset by mouth control
    jaw.setMinMax(10, 25);
    eyeX.setMinMax(60, 100);
    eyeY.setMinMax(50, 100);

    neck.setRest(90);
    rothead.setRest(90);
    jaw.setRest(10);
    eyeX.setRest(80);
    eyeY.setRest(90);
    
    setVelocity(45.0,45.0,45.0,45.0,45.0);

  }

  /**
   * attach all the servos - this must be re-entrant and accomplish the
   * re-attachment when servos are detached
   * 
   * @return
   */
  public boolean attach() {
    sleep(InMoov.attachPauseMs);
    eyeX.attach();
    sleep(InMoov.attachPauseMs);
    eyeY.attach();
    sleep(InMoov.attachPauseMs);
    jaw.attach();
    sleep(InMoov.attachPauseMs);
    rothead.attach();
    sleep(InMoov.attachPauseMs);
    neck.attach();

    return true;
  }

  // FIXME - should be broadcastServoState
  @Override
  public void broadcastState() {
    // notify the gui
    rothead.broadcastState();
    neck.broadcastState();
    eyeX.broadcastState();
    eyeY.broadcastState();
    jaw.broadcastState();
  }

  // FIXME - make interface for Arduino / Servos !!!
  public boolean connect(String port) throws Exception {
    arduino.connect(port);
    neck.attach(arduino, 12, neck.getRest(), neck.getVelocity());
    rothead.attach(arduino, 13, rothead.getRest(), rothead.getVelocity());
    jaw.attach(arduino, 26, jaw.getRest(), jaw.getVelocity());
    eyeX.attach(arduino, 22, eyeX.getRest(), eyeX.getVelocity());
    eyeY.attach(arduino, 24, eyeY.getRest(), eyeY.getVelocity());
    broadcastState();
    return true;
  }

  public void detach() {
    sleep(InMoov.attachPauseMs);
    if (rothead != null) {
      rothead.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (neck != null) {
      neck.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (eyeX != null) {
      eyeX.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (eyeY != null) {
      eyeY.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (jaw != null) {
      jaw.detach();
    }
  }

  public long getLastActivityTime() {

    long lastActivityTime = Math.max(rothead.getLastActivityTime(), neck.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyeX.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyeY.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, jaw.getLastActivityTime());
    return lastActivityTime;
  }

  public String getScript(String inMoovServiceName) {
    return String.format("%s.moveHead(%d,%d,%d,%d,%d)\n", inMoovServiceName, neck.getPos(), rothead.getPos(), eyeX.getPos(), eyeY.getPos(), jaw.getPos());
  }

  public boolean isAttached() {
    boolean attached = false;

    attached |= rothead.isAttached();
    attached |= neck.isAttached();
    attached |= eyeX.isAttached();
    attached |= eyeY.isAttached();
    attached |= jaw.isAttached();

    return attached;
  }

  public boolean isValid() {
    rothead.moveTo(rothead.getRest() + 2);
    neck.moveTo(neck.getRest() + 2);
    eyeX.moveTo(eyeX.getRest() + 2);
    eyeY.moveTo(eyeY.getRest() + 2);
    jaw.moveTo(jaw.getRest() + 2);
    return true;
  }

  public void lookAt(Double x, Double y, Double z) {
    Double distance = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0));
    Double rotation = Math.toDegrees(Math.atan(y / x));
    Double colatitude = Math.toDegrees(Math.acos(z / distance));
    System.out.println(distance);
    System.out.println(rotation);
    System.out.println(colatitude);
    log.info(String.format("object distance is %f,rothead servo %f,neck servo %f ", distance, rotation, colatitude));
  }

  public void moveTo(Integer neck, Integer rothead) {
    moveTo(neck, rothead, null, null, null);
  }

  public void moveTo(Integer neck, Integer rothead, Integer eyeX, Integer eyeY) {
    moveTo(neck, rothead, eyeX, eyeY, null);
  }

  public void moveTo(Integer neck, Integer rothead, Integer eyeX, Integer eyeY, Integer jaw) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("head.moveTo %d %d %d %d %d", neck, rothead, eyeX, eyeY, jaw));
    }
    this.rothead.moveTo(rothead);
    this.neck.moveTo(neck);
    if (eyeX != null)
      this.eyeX.moveTo(eyeX);
    if (eyeY != null)
      this.eyeY.moveTo(eyeY);
    if (jaw != null)
      this.jaw.moveTo(jaw);
  }

  public void release() {
    detach();
    rothead.releaseService();
    neck.releaseService();
    eyeX.releaseService();
    eyeY.releaseService();
    jaw.releaseService();
  }

  public void rest() {
    // initial positions
    setSpeed(1.0, 1.0, 1.0, 1.0, 1.0);
    rothead.rest();
    neck.rest();
    eyeX.rest();
    eyeY.rest();
    jaw.rest();
  }

  @Override
  public boolean save() {
    super.save();
    rothead.save();
    neck.save();
    eyeX.save();
    eyeY.save();
    jaw.save();
    return true;
  }

  public void setLimits(int headXMin, int headXMax, int headYMin, int headYMax, int eyeXMin, int eyeXMax, int eyeYMin, int eyeYMax, int jawMin, int jawMax) {
    rothead.setMinMax(headXMin, headXMax);
    neck.setMinMax(headYMin, headYMax);
    eyeX.setMinMax(eyeXMin, eyeXMax);
    eyeY.setMinMax(eyeYMin, eyeYMax);
    jaw.setMinMax(jawMin, jawMax);
  }

  // ----- initialization end --------
  // ----- movements begin -----------

  public void setpins(int headXPin, int headYPin, int eyeXPin, int eyeYPin, int jawPin) {
    log.info(String.format("setPins %d %d %d %d %d", headXPin, headYPin, eyeXPin, eyeYPin, jawPin));

    /*
    rothead.setPin(headXPin);
    neck.setPin(headYPin);
    eyeX.setPin(eyeXPin);
    eyeY.setPin(eyeYPin);
    jaw.setPin(jawPin);
    */

    arduino.servoAttachPin(rothead, headXPin);
    arduino.servoAttachPin(neck, headYPin);
    arduino.servoAttachPin(eyeX, eyeXPin);
    arduino.servoAttachPin(eyeY, eyeYPin);
    arduino.servoAttachPin(jaw, jawPin);

  }

  public void setSpeed(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s setSpeed %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed));
    }
    rothead.setSpeed(headXSpeed);
    neck.setSpeed(headYSpeed);
    // it's possible to pass null for the eye and jaw speeds
    if (eyeXSpeed != null) {
      eyeX.setSpeed(eyeXSpeed);
    }
    if (eyeYSpeed != null) {
      eyeY.setSpeed(eyeYSpeed);
    }
    if (jawSpeed != null) {
      jaw.setSpeed(jawSpeed);
    }

  }

  @Override
  public void startService() {
    super.startService();
    arduino.startService();
    jaw.startService();
    eyeX.startService();
    eyeY.startService();
    rothead.startService();
    neck.startService();
  }

  /*
   * public boolean load(){ super.load(); rothead.load(); neck.load();
   * eyeX.load(); eyeY.load(); jaw.load(); return true; }
   */

  public void test() {

    if (arduino == null) {
      error("arduino is null");
    }

    if (!arduino.isConnected()) {
      error("arduino not connected");
    }

    rothead.moveTo(rothead.getPos() + 2);
    neck.moveTo(neck.getPos() + 2);
    eyeX.moveTo(eyeX.getPos() + 2);
    eyeY.moveTo(eyeY.getPos() + 2);
    jaw.moveTo(jaw.getPos() + 2);
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

    ServiceType meta = new ServiceType(InMoovHead.class.getCanonicalName());
    meta.addDescription("InMoov Head Service");
    meta.addCategory("robot");

    meta.addPeer("jaw", "Servo", "Jaw servo");
    meta.addPeer("eyeX", "Servo", "Eyes pan servo");
    meta.addPeer("eyeY", "Servo", "Eyes tilt servo");
    meta.addPeer("rothead", "Servo", "Head pan servo");
    meta.addPeer("neck", "Servo", "Head tilt servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");

    return meta;
  }

  public void setVelocity(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s setVelocity %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed));
    }
    rothead.setVelocity(headXSpeed);
    neck.setVelocity(headYSpeed);
    // it's possible to pass null for the eye and jaw speeds
    if (eyeXSpeed != null) {
      eyeX.setVelocity(eyeXSpeed);
    }
    if (eyeYSpeed != null) {
      eyeY.setVelocity(eyeYSpeed);
    }
    if (jawSpeed != null) {
      jaw.setVelocity(jawSpeed);
    }

  }

}
