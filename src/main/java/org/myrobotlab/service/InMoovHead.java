package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * InMoovHead - This is the inmoov head service. This service controls the
 * servos for the following: jaw, eyeX, eyeY, rothead and neck.
 * 
 */
public class InMoovHead extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovHead.class);

  transient public ServoControl jaw;
  transient public ServoControl eyeX;
  transient public ServoControl eyeY;
  transient public ServoControl rothead;
  transient public ServoControl neck;
  transient public ServoControl rollNeck;
  transient public ServoController controller;

  public InMoovHead(String n, String id) {
    super(n, id);
    jaw = (ServoControl) createPeer("jaw");
    eyeX = (ServoControl) createPeer("eyeX");
    eyeY = (ServoControl) createPeer("eyeY");
    rothead = (ServoControl) createPeer("rothead");
    neck = (ServoControl) createPeer("neck");
    rollNeck = (ServoControl) createPeer("rollNeck");
    initServoDefaults();
  }
  
  public void releaseService() {
    try {
      disable();
      releasePeers();
      super.releaseService(); 
    } catch (Exception e) {
      error(e);
    }
  }

  
  public List<String> getServoNames() {
    List<String> servos = new ArrayList<>();
    
    if (jaw != null) {
      servos.add(jaw.getName());
    }
    if (eyeX != null) {
      servos.add(eyeX.getName());
    }
    if (eyeY != null) {
      servos.add(eyeY.getName());
    }
    if (rothead != null) {
      servos.add(rothead.getName());
    }
    if (neck != null) {
      servos.add(neck.getName());
    }
    if (rollNeck != null) {
      servos.add(rollNeck.getName());
    }  
    Collections.sort(servos);
    return servos;
  }

  private void initServoDefaults() {
    neck.setMinMax((double)20, (double)160);
    rollNeck.setMinMax((double)20, (double)160);
    rothead.setMinMax((double)30, (double)150);
    // reset by mouth control
    jaw.setMinMax((double)10, (double)25);
    eyeX.setMinMax((double)60, (double)120);
    eyeY.setMinMax((double)60, (double)120);
    neck.setRest(90.0);
    neck.setPosition(90.0);
    rollNeck.setRest(90.0);
    rollNeck.setPosition(90.0);
    rothead.setRest(90.0);
    rothead.setPosition(90.0);
    jaw.setRest(10.0);
    jaw.setPosition(10.0);
    eyeX.setRest(90.0);
    eyeX.setPosition(90.0);
    eyeY.setRest(90.0);
    eyeY.setPosition(90.0);

    setVelocity(45.0, 45.0, -1.0, -1.0, -1.0, 45.0);
  }

  /*
   * attach all the servos - this must be re-entrant and accomplish the
   * re-attachment when servos are detached
   */
  @Deprecated
  public boolean attach() {
    log.warn("attach deprecated please use enable");
    sleep(InMoov.attachPauseMs);
    eyeX.enable();
    sleep(InMoov.attachPauseMs);
    eyeY.enable();
    sleep(InMoov.attachPauseMs);
    jaw.enable();
    sleep(InMoov.attachPauseMs);
    rothead.enable();
    sleep(InMoov.attachPauseMs);
    neck.enable();
    sleep(InMoov.attachPauseMs);
    rollNeck.enable();
    sleep(InMoov.attachPauseMs);
    return true;
  }

  public boolean enable() {
    sleep(InMoov.attachPauseMs);
    eyeX.enable();
    sleep(InMoov.attachPauseMs);
    eyeY.enable();
    sleep(InMoov.attachPauseMs);
    jaw.enable();
    sleep(InMoov.attachPauseMs);
    rothead.enable();
    sleep(InMoov.attachPauseMs);
    neck.enable();
    sleep(InMoov.attachPauseMs);
    rollNeck.enable();
    sleep(InMoov.attachPauseMs);
    return true;
  }

  // FIXME - should be broadcastServoState
  @Override
  public void broadcastState() {
    // notify the gui
    rothead.broadcastState();
    rollNeck.broadcastState();
    neck.broadcastState();
    eyeX.broadcastState();
    eyeY.broadcastState();
    jaw.broadcastState();
  }

  // FIXME - make interface for Arduino / Servos !!!
  public boolean connect(String port) throws Exception {
    return connect(port, 12, 13, 22, 24, 26, 30);

  }

  public boolean connect(String port, Integer neckPin, Integer rotheadPin, Integer eyeXPin, Integer eyeYPin, Integer jawPin, Integer rollNeckPin) throws Exception {

    if (controller instanceof PortConnector) {
      PortConnector arduino = (PortConnector) controller;
      arduino.connect(port);
      if (!arduino.isConnected()) {
        error("controller for head is not connected");
      }
    }
    
    neck.setPin(neckPin);
    rothead.setPin(rotheadPin);
    eyeX.setPin(eyeXPin);
    eyeY.setPin(eyeYPin);
    jaw.setPin(jawPin);
    rollNeck.setPin(rollNeckPin);
    
    neck.attach(controller);
    rothead.attach(controller);
    jaw.attach(controller);
    eyeX.attach(controller);
    eyeY.attach(controller);
    rollNeck.attach(controller);
    
    broadcastState();

    return true;
  }

  @Deprecated
  public void detach() {
    log.warn("detach deprecated please use disable");
    disable();
  }

  public void disable() {
    sleep(InMoov.attachPauseMs);
    if (rothead != null) {
      rothead.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (neck != null) {
      neck.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (eyeX != null) {
      eyeX.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (eyeY != null) {
      eyeY.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (jaw != null) {
      jaw.disable();
    }
    if (rollNeck != null) {
      rollNeck.disable();
    }
  }

  public long getLastActivityTime() {

    long lastActivityTime = Math.max(rothead.getLastActivityTime(), neck.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyeX.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyeY.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, jaw.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, rollNeck.getLastActivityTime());
    return lastActivityTime;
  }

  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveHead(%.2f,%.2f,%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, neck.getPos(), rothead.getPos(), eyeX.getPos(), eyeY.getPos(),
        jaw.getPos(), rollNeck.getPos());
  }


  public boolean isValid() {
    rothead.moveTo(rothead.getRest() + 2);
    neck.moveTo(neck.getRest() + 2);
    eyeX.moveTo(eyeX.getRest() + 2);
    eyeY.moveTo(eyeY.getRest() + 2);
    jaw.moveTo(jaw.getRest() + 2);
    rollNeck.moveTo(rollNeck.getRest() + 2);
    return true;
  }

  public void lookAt(Double x, Double y, Double z) {
    Double distance = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0));
    Double rotation = Math.toDegrees(Math.atan(y / x));
    Double colatitude = Math.toDegrees(Math.acos(z / distance));
    System.out.println(distance);
    System.out.println(rotation);
    System.out.println(colatitude);
    log.info("object distance is {},rothead servo {},neck servo {} ", distance, rotation, colatitude);
  }

  public void moveTo(double neck, double rothead) {
    moveTo(neck, rothead, null, null, null, null);
  }

  // TODO IK head moveTo ( neck + rollneck + rothead ? )
  public void moveTo(Double neck, Double rothead, Double rollNeck) {
    moveTo(neck, rothead, null, null, null, rollNeck);
  }

  public void moveTo(double neck, double rothead, double eyeX, double eyeY) {
    moveTo(neck, rothead, eyeX, eyeY, null, null);
  }

  public void moveTo(double neck, double rothead, double eyeX, double eyeY, double jaw) {
    moveTo(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveTo(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    if (log.isDebugEnabled()) {
      log.debug("head.moveTo {} {} {} {} {} {}", neck, rothead, eyeX, eyeY, jaw, rollNeck);
    }
    if (rothead != null)
      this.rothead.moveTo(rothead);
    if (neck != null)
      this.neck.moveTo(neck);
    if (eyeX != null)
      this.eyeX.moveTo(eyeX);
    if (eyeY != null)
      this.eyeY.moveTo(eyeY);
    if (jaw != null)
      this.jaw.moveTo(jaw);
    if (rollNeck != null)
      this.rollNeck.moveTo(rollNeck);
  }

  public void moveToBlocking(double neck, double rothead) {
    moveToBlocking(neck, rothead, null, null, null, null);
  }

  public void moveToBlocking(double neck, double rothead, Double rollNeck) {
    moveToBlocking(neck, rothead, null, null, null, rollNeck);
  }

  public void moveToBlocking(double neck, double rothead, double eyeX, double eyeY) {
    moveToBlocking(neck, rothead, eyeX, eyeY, null, null);
  }

  public void moveToBlocking(double neck, double rothead, double eyeX, double eyeY, double jaw) {
    moveToBlocking(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveToBlocking(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    log.info("init {} moveToBlocking ", getName());
    moveTo(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    waitTargetPos();
    log.info("end {} moveToBlocking ", getName());
  }

  public void waitTargetPos() {
    neck.waitTargetPos();
    rothead.waitTargetPos();
    eyeX.waitTargetPos();
    eyeY.waitTargetPos();
    jaw.waitTargetPos();
    rollNeck.waitTargetPos();
  }

  public void release() {
    disable();
  }

  public void rest() {
    // initial positions
    // setSpeed(1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    rothead.rest();
    neck.rest();
    eyeX.rest();
    eyeY.rest();
    jaw.rest();
    rollNeck.rest();
  }

  @Override
  public boolean save() {
    super.save();
    rothead.save();
    neck.save();
    eyeX.save();
    eyeY.save();
    jaw.save();
    rollNeck.save();
    return true;
  }

  @Deprecated
  public void enableAutoDisable(Boolean rotheadParam, Boolean neckParam, Boolean rollNeckParam) {
    setAutoDisable(rotheadParam, neckParam, rollNeckParam);
  }

  @Deprecated
  public void enableAutoDisable(Boolean param) {
    setAutoDisable(param);
  }

  public void setAutoDisable(Boolean rotheadParam, Boolean neckParam, Boolean rollNeckParam) {
    rothead.setAutoDisable(rotheadParam);
    rollNeck.setAutoDisable(rollNeckParam);
    neck.setAutoDisable(neckParam);
  }

  public void setAutoDisable(Boolean param) {
    rothead.setAutoDisable(param);
    neck.setAutoDisable(param);
    eyeX.setAutoDisable(param);
    eyeY.setAutoDisable(param);
    jaw.setAutoDisable(param);
    rollNeck.setAutoDisable(param);
  }

  @Deprecated
  public void enableAutoEnable(Boolean rotheadParam, Boolean neckParam, Boolean rollNeckParam) {
    enableAutoEnable(true);
  }

  @Deprecated
  public void enableAutoEnable(Boolean param) {
  }

  public void setLimits(double headXMin, double headXMax, double headYMin, double headYMax, double eyeXMin, double eyeXMax, double eyeYMin, double eyeYMax, double jawMin,
      double jawMax, double rollNeckMin, double rollNeckMax) {
    rothead.setMinMax(headXMin, headXMax);
    neck.setMinMax(headYMin, headYMax);
    eyeX.setMinMax(eyeXMin, eyeXMax);
    eyeY.setMinMax(eyeYMin, eyeYMax);
    jaw.setMinMax(jawMin, jawMax);
    rollNeck.setMinMax(rollNeckMin, rollNeckMax);
  }

  // ----- initialization end --------
  // ----- movements begin -----------

  public void setpins(int headXPin, int headYPin, int eyeXPin, int eyeYPin, int jawPin, int rollNeckPin) {
    log.info("setPins {} {} {} {} {} {}", headXPin, headYPin, eyeXPin, eyeYPin, jawPin, rollNeckPin);
    try {
      controller.attach(rothead, headXPin);
      controller.attach(neck, headYPin);
      controller.attach(eyeX, eyeXPin);
      controller.attach(eyeY, eyeYPin);
      controller.attach(jaw, jawPin);
      controller.attach(rollNeck, rollNeckPin);
    } catch (Exception e) {
      error(e);
    }

  }

  public void setSpeed(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {

    setSpeed(headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, null);

  }

  @Deprecated
  public void setSpeed(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s setSpeed %.2f %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed));
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
    if (rollNeckSpeed != null) {
      jaw.setSpeed(rollNeckSpeed);
    }

  }

  @Override
  public void startService() {
    super.startService();

    // part of the "start" life-cycle - these services "check" on late binding
    // meaning if "stuff is already setup - we don't mess with it", otherwise
    // configure with defaults

    if (controller == null) {
      controller = (ServoController) startPeer("arduino");
    }

    startPeer("jaw");
    startPeer("eyeX");
    startPeer("eyeY");
    startPeer("rothead");
    startPeer("neck");
    startPeer("rollNeck");
    
    if (jaw == null) {
      jaw = (ServoControl) startPeer("jaw");
      jaw.setMinMax(10.0, 25.0);
      jaw.setRest(10.0);
    }
    if (eyeX == null) {
      eyeX = (ServoControl) startPeer("eyeX");
      eyeX.setMinMax(60.0, 120.0);
      eyeX.setRest(90.0);
    }
    if (eyeY == null) {
      eyeY = (ServoControl) startPeer("eyeY");
      eyeY.setMinMax(60.0, 120.0);
      eyeY.setRest(90.0);
    }
    if (rollNeck == null) {
      rollNeck = (ServoControl) startPeer("rollNeck");
      rollNeck.setMinMax(20.0, 160.0);
      rollNeck.setRest(90.0);

    }
    if (neck == null) {
      neck = (ServoControl) startPeer("neck");
      neck.setMinMax(20.0, 160.0);
      neck.setRest(90.0);
    }

    if (rothead == null) {
      rothead = (ServoControl) startPeer("rothead");
      rothead.setMinMax(20.0, 160.0);
      rothead.setRest(90.0);
    }

    setVelocity(45.0, 45.0, null, null, null, 45.0);
  }

  public void test() {

    if (controller == null) {
      error("arduino is null");
    }

    /*
     * FIXME - !!! => cannot do this "here" ??? if (!arduino.isConnected()) {
     * error("arduino not connected"); }
     */

    rothead.moveTo(rothead.getPos() + 2);
    neck.moveTo(neck.getPos() + 2);
    eyeX.moveTo(eyeX.getPos() + 2);
    eyeY.moveTo(eyeY.getPos() + 2);
    jaw.moveTo(jaw.getPos() + 2);
    rollNeck.moveTo(rollNeck.getPos() + 2);
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
    meta.addPeer("rollNeck", "Servo", "rollNeck Mod servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");

    return meta;
  }

  public void setVelocity(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {

    setVelocity(headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, null);

  }

  public void setVelocity(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s setVelocity %.2f %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed));
    }
    rothead.setSpeed(headXSpeed);
    neck.setSpeed(headYSpeed);
    eyeX.setSpeed(eyeXSpeed);
    eyeY.setSpeed(eyeYSpeed);
    jaw.setSpeed(jawSpeed);
    rollNeck.setSpeed(rollNeckSpeed);
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      String leftPort = "COM3";

      VirtualArduino vleft = (VirtualArduino) Runtime.start("vleft", "VirtualArduino");
      vleft.connect("COM3");
      Runtime.start("gui", "SwingGui");

      InMoovHead head = (InMoovHead) Runtime.start("head", "InMoovHead");
      head.connect("COM3");

      log.info(head.getScript("i01"));

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void setController(ServoController controller) {
    this.controller = controller;
  }

}
