package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.LeapData;
import org.myrobotlab.service.data.LeapHand;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.LeapDataListener;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * InMoovHand - The Hand sub service for the InMoov Robot. This service has 6
 * servos controlled by an ServoController.
 * thumb,index,majeure,ringFinger,pinky, and wrist
 * 
 * There is also leap motion support.
 */
public class InMoov2Hand extends Service implements LeapDataListener, PinArrayListener {

  public final static Logger log = LoggerFactory.getLogger(InMoov2Hand.class);

  private static final long serialVersionUID = 1L;

  /**
   * peer services
   */
  transient public LeapMotion leap;
  transient public ServoController controller;
  transient public ServoControl thumb;
  transient public ServoControl index;
  transient public ServoControl majeure;
  transient public ServoControl ringFinger;
  transient public ServoControl pinky;
  transient public ServoControl wrist;

  // The pins for the finger tip sensors
  public String[] sensorPins = new String[] { "A0", "A1", "A2", "A3", "A4" };
  // public int[] sensorLastValues = new int[] {0,0,0,0,0};
  public boolean sensorsEnabled = false;
  public int[] sensorThresholds = new int[] { 500, 500, 500, 500, 500 };

  /**
   * list of names of possible controllers
   */
  public List<String> controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
  public String controllerName;

  boolean isAttached = false;

  private int sensorPin;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      InMoov i01 = (InMoov) Runtime.start("i01", "InMoov");
      i01.startRightHand("COM15");

      ServoController controller = (ServoController) Runtime.getService("i01.right");
      // arduino.pinMode(13, ServoController.OUTPUT);
      // arduino.digitalWrite(13, 1);

      InMoov2Hand rightHand = (InMoov2Hand) Runtime.start("r01", "InMoov2Hand");// InMoovHand("r01");
      Runtime.createAndStart("gui", "SwingGui");

      Runtime.createAndStart("webgui", "WebGui");
      // rightHand.connect("COM12"); TEST RECOVERY !!!
      rightHand.close();
      rightHand.open();
      rightHand.openPinch();
      rightHand.closePinch();
      rightHand.rest();
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public InMoov2Hand(String n, String id) {
    super(n, id);

    // FIXME - NO DIRECT REFERENCES ALL PUB SUB

  }

  public void startService() {
    super.startService();
    // FIXME - creatPeers()
    startPeers();
    // createPeers()

    thumb.setPin(2);
    index.setPin(3);
    majeure.setPin(4);
    ringFinger.setPin(5);
    pinky.setPin(6);
    wrist.setPin(7);

    /*
     * thumb.setSensorPin(A0); index.setSensorPin(A1); majeure.setSensorPin(A2);
     * ringFinger.setSensorPin(A3); pinky.setSensorPin(A4);
     */

    // TOOD: what are the initial velocities?
    // Initial rest positions?
    thumb.setRest(2.0);
    thumb.setPosition(2.0);
    index.setRest(2.0);
    index.setPosition(2.0);
    majeure.setRest(2.0);
    majeure.setPosition(2.0);
    ringFinger.setRest(2.0);
    ringFinger.setPosition(2.0);
    pinky.setRest(2.0);
    pinky.setPosition(2.0);
    wrist.setRest(90.0);
    wrist.setPosition(90.0);

    setSpeed(45.0, 45.0, 45.0, 45.0, 45.0, 45.0);
  }

  public void bird() {
    moveTo(150.0, 180.0, 0.0, 180.0, 180.0, 90.0);
  }

  public void onRegistered(Registration s) {
    refreshControllers();
    broadcastState();
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
    return controllers;
  }

  // @Override
  public ServoController getController() {
    return controller;
  }

  public String getControllerName() {
    String controlerName = null;
    if (controller != null) {
      controlerName = controller.getName();
    }
    return controlerName;
  }

  public boolean isAttached() {
    if (controller != null) {
      if (((Arduino) controller).getDeviceId((Attachable) this) != null) {
        isAttached = true;
        return true;
      }
      controller = null;
    }
    isAttached = false;
    return false;
  }

  @Override
  public void broadcastState() {
    if (thumb != null)
      thumb.broadcastState();
    if (index != null)
      index.broadcastState();
    if (majeure != null)
      majeure.broadcastState();
    if (ringFinger != null)
      ringFinger.broadcastState();
    if (pinky != null)
      pinky.broadcastState();
    if (wrist != null)
      wrist.broadcastState();
  }

  public void close() {
    moveTo(130, 180, 180, 180, 180);
  }

  public void closePinch() {
    moveTo(130, 140, 180, 180, 180);
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

  public void count() {
    one();
    sleep(1);
    two();
    sleep(1);
    three();
    sleep(1);
    four();
    sleep(1);
    five();
  }

  public void devilHorns() {
    moveTo(150.0, 0.0, 180.0, 180.0, 0.0, 90.0);
  }

  public void disable() {
    thumb.disable();
    index.disable();
    majeure.disable();
    ringFinger.disable();
    pinky.disable();
    wrist.disable();
  }

  public boolean enable() {

    thumb.enable();

    index.enable();

    majeure.enable();

    ringFinger.enable();

    pinky.enable();

    wrist.enable();
    return true;
  }

  @Deprecated
  public void enableAutoDisable(Boolean param) {
    setAutoDisable(param);
  }

  @Deprecated
  public void enableAutoEnable(Boolean param) {
  }

  public void five() {
    open();
  }

  public void four() {
    moveTo(150.0, 0.0, 0.0, 0.0, 0.0, 90.0);
  }

  public void fullSpeed() {
    thumb.fullSpeed();
    index.fullSpeed();
    majeure.fullSpeed();
    ringFinger.fullSpeed();
    pinky.fullSpeed();
    wrist.fullSpeed();
  }

  /**
   * this method returns the analog pins that the hand is listening to. The
   * InMoovHand listens on analog pins A0-A4 for the finger tip sensors.
   * 
   */
  @Override
  public String[] getActivePins() {
    // TODO Auto-generated method stub
    // for the InMoov hand, we're just going to say A0 - A4 ... for now..
    return sensorPins;
  }

  public long getLastActivityTime() {

    long lastActivityTime = Math.max(index.getLastActivityTime(), thumb.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, index.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, majeure.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, ringFinger.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, pinky.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, wrist.getLastActivityTime());

    return lastActivityTime;

  }

  @Deprecated /* use LangUtils */
  public String getScript(String inMoovServiceName) {
    String side = getName().contains("left") ? "left" : "right";
    return String.format(Locale.ENGLISH, "%s.moveHand(\"%s\",%.2f,%.2f,%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, side, thumb.getCurrentInputPos(), index.getCurrentInputPos(),
        majeure.getCurrentInputPos(), ringFinger.getCurrentInputPos(), pinky.getCurrentInputPos(), wrist.getCurrentInputPos());
  }

  public void hangTen() {
    moveTo(0.0, 180.0, 180.0, 180.0, 0.0, 90.0);
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    thumb.map(minX, maxX, minY, maxY);
    index.map(minX, maxX, minY, maxY);
    majeure.map(minX, maxX, minY, maxY);
    ringFinger.map(minX, maxX, minY, maxY);
    pinky.map(minX, maxX, minY, maxY);
  }

  // TODO - waving thread fun
  public void moveTo(double thumb, double index, double majeure, double ringFinger, double pinky) {
    moveTo(thumb, index, majeure, ringFinger, pinky, null);
  }

  public void moveTo(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    if (log.isDebugEnabled()) {
      log.debug("{}.moveTo {} {} {} {} {} {}", getName(), thumb, index, majeure, ringFinger, pinky, wrist);
    }

    this.thumb.moveTo(thumb);
    this.index.moveTo(index);
    this.majeure.moveTo(majeure);
    this.ringFinger.moveTo(ringFinger);
    this.pinky.moveTo(pinky);
    this.wrist.moveTo(wrist);
  }

  public void moveToBlocking(double thumb, double index, double majeure, double ringFinger, double pinky) {
    moveToBlocking(thumb, index, majeure, ringFinger, pinky, null);
  }

  public void moveToBlocking(double thumb, double index, double majeure, double ringFinger, double pinky, Double wrist) {
    log.info("init {} moveToBlocking ", getName());
    moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
    waitTargetPos();
    log.info("end {} moveToBlocking ", getName());
  }

  public void ok() {
    moveTo(150.0, 180.0, 0.0, 0.0, 0.0, 90.0);
  }

  public void one() {
    moveTo(150.0, 0.0, 180.0, 180.0, 180.0, 90.0);
  }

  public void attach(String controllerName, int sensorPin) throws Exception {
    attach((ServoController) Runtime.getService(controllerName), sensorPin);
  }

  public void attach(String controllerName, String sensorPin) throws Exception {
    attach((ServoController) Runtime.getService(controllerName), Integer.parseInt(sensorPin));
  }

  public void attach(ServoController controller, int sensorPin) {
    try {
      if (controller == null) {
        error("setting null as controller");
        return;
      }
      if (isAttached) {
        log.info("Sensor already attached");
        return;
      }

      this.sensorPin = sensorPin;

      controller.attach(controller);

      log.info("{} setController {}", getName(), controller.getName());
      this.controller = controller;
      controllerName = this.controller.getName();
      isAttached = true;
      broadcastState();
    } catch (Exception e) {
      error(e);
    }
  }

  public void detach(ServoController controller) {
    // let the controller you want to detach this device
    if (controller != null) {
      controller.detach(this);
    }
    // setting controller reference to null
    this.controller = null;
    isAttached = false;
    refreshControllers();
    broadcastState();
  }

  public void refresh() {
    broadcastState();
  }

  @Override
  public LeapData onLeapData(LeapData data) {
    String side = getName().contains("left") ? "left" : "right";
    if (!data.frame.isValid()) {
      // TODO: we could return void here? not sure
      // who wants the return value form this method.
      log.info("Leap data frame not valid.");
      return data;
    }
    LeapHand h;
    if ("right".equalsIgnoreCase(side)) {
      if (data.frame.hands().rightmost().isValid()) {
        h = data.rightHand;
      } else {
        log.info("Right hand frame not valid.");
        // return this hand isn't valid
        return data;
      }
    } else if ("left".equalsIgnoreCase(side)) {
      if (data.frame.hands().leftmost().isValid()) {
        h = data.leftHand;
      } else {
        log.info("Left hand frame not valid.");
        // return this frame isn't valid.
        return data;
      }
    } else {
      // side could be null?
      log.info("Unknown Side or side not set on hand (Side = {})", side);
      // we can default to the right side?
      // TODO: come up with a better default or at least document this
      // behavior.
      if (data.frame.hands().rightmost().isValid()) {
        h = data.rightHand;
      } else {
        log.info("Right(unknown) hand frame not valid.");
        // return this hand isn't valid
        return data;
      }
    }

    return data;
  }

  // ----- initialization end --------
  // ----- movements begin -----------

  // FIXME - use pub/sub attach to set this up without having this method !
  @Override
  public void onPinArray(PinData[] pindata) {

    log.info("On Pin Data: {}", pindata.length);
    if (!sensorsEnabled)
      return;
    // just return ? TOOD: maybe still track the last read values...
    // TODO : change the interface to get a map of pin data, keyed off the name.
    // ?
    for (PinData pin : pindata) {
      log.info("Pin Data: {}", pin);
      // p
      // if (sensorPins.contains(pin.pin)) {
      // // it's one of our finger pins.. let's operate on it.
      // log.info("Pin Data : {} value {}", pin.pin, pin.value );
      // if (sensorPins[0].equalsIgnoreCase(pin.pin)) {
      // // thumb / A0
      // // here we want to test the pin state.. and potentially take an action
      // // based on the updated sensor pin state
      // if (pin.value > sensorThresholds[0])
      // thumb.stop();
      // } else if (sensorPins[1].equalsIgnoreCase(pin.pin)) {
      // // index / A1
      // if (pin.value > sensorThresholds[1])
      // index.stop();
      //
      // } else if (sensorPins[2].equalsIgnoreCase(pin.pin)) {
      // // middle / A2
      // if (pin.value > sensorThresholds[2])
      // majeure.stop();
      //
      // } else if (sensorPins[3].equalsIgnoreCase(pin.pin)) {
      // // ring / A3
      // if (pin.value > sensorThresholds[3])
      // ringFinger.stop();
      //
      // } else if (sensorPins[4].equalsIgnoreCase(pin.pin)) {
      // // pinky / A4
      // if (pin.value > sensorThresholds[4])
      // pinky.stop();
      // }
      // }
    }
  }

  public void open() {
    rest();
  }

  public void openPinch() {
    moveTo(0, 0, 180, 180, 180);
  }

  public void release() {
    disable();
  }

  public void rest() {
    thumb.rest();
    index.rest();
    majeure.rest();
    ringFinger.rest();
    pinky.rest();
    wrist.rest();
  }

  @Override
  public boolean save() {
    super.save();
    thumb.save();
    index.save();
    majeure.save();
    ringFinger.save();
    pinky.save();
    wrist.save();
    return true;
  }

  @Deprecated
  public boolean loadFile(String file) {
    File f = new File(file);
    Python p = (Python) Runtime.getService("python");
    log.info("Loading  Python file {}", f.getAbsolutePath());
    if (p == null) {
      log.error("Python instance not found");
      return false;
    }
    String script = null;
    try {
      script = FileIO.toString(f.getAbsolutePath());
    } catch (IOException e) {
      log.error("IO Error loading file : ", e);
      return false;
    }
    // evaluate the scripts in a blocking way.
    boolean result = p.exec(script, true);
    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
      return false;
    } else {
      log.debug("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }

  public void setAutoDisable(Boolean param) {
    thumb.setAutoDisable(param);
    index.setAutoDisable(param);
    majeure.setAutoDisable(param);
    ringFinger.setAutoDisable(param);
    pinky.setAutoDisable(param);
    wrist.setAutoDisable(param);
  }

  public void setPins(int thumbPin, int indexPin, int majeurePin, int ringFingerPin, int pinkyPin, int wristPin) {
    log.info("setPins {} {} {} {} {} {}", thumbPin, indexPin, majeurePin, ringFingerPin, pinkyPin, wristPin);
    thumb.setPin(thumbPin);
    index.setPin(indexPin);
    majeure.setPin(majeurePin);
    ringFinger.setPin(ringFingerPin);
    pinky.setPin(pinkyPin);
    wrist.setPin(wristPin);
  }

  public void setSensorPins(int thumbSensorPin, int indexSensorPin, int majeureSensorPin, int ringFingerSensorPin, int pinkySensorPin) {
    log.info("setSensorPins {} {} {} {} {}", thumbSensorPin, indexSensorPin, majeureSensorPin, ringFingerSensorPin, pinkySensorPin);
    /*
     * thumb.setSensorPin(thumbSensorPin); index.setSensorPin(indexSensorPin);
     * majeure.setSensorPin(majeureSensorPin);
     * ringFinger.setSensorPin(ringFingerSensorPin);
     * pinky.setSensorPin(pinkySensorPin);
     */
  }

  public void setRest(double thumb, double index, double majeure, double ringFinger, double pinky) {
    setRest(thumb, index, majeure, ringFinger, pinky, null);
  }

  public void setRest(double thumb, double index, double majeure, double ringFinger, double pinky, Double wrist) {
    log.info("setRest {} {} {} {} {} {}", thumb, index, majeure, ringFinger, pinky, wrist);
    this.thumb.setRest(thumb);
    this.index.setRest(index);
    this.majeure.setRest(majeure);
    this.ringFinger.setRest(ringFinger);
    this.pinky.setRest(pinky);
    if (wrist != null) {
      this.wrist.setRest(wrist);
    }
  }

  /**
   * @param pins Set the array of pins that should be listened to.
   * 
   */
  public void setSensorPins(String[] pins) {
    // TODO, this should probably be a sorted set.. and sensorPins itself should
    // probably be a map to keep the mapping of pin to finger
    this.sensorPins = pins;
  }

  public void setSpeed(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {

    this.thumb.setSpeed(thumb);
    this.index.setSpeed(index);
    this.majeure.setSpeed(majeure);
    this.ringFinger.setSpeed(ringFinger);
    this.pinky.setSpeed(pinky);
    this.wrist.setSpeed(wrist);
  }

  @Deprecated
  public void setVelocity(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    log.warn("setspeed deprecated please use setSpeed");
    setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
  }

  // FIXME - if multiple systems are dependent on the ServoControl map and
  // limits to be a certain value
  // leap should change its output, and leave the map and limits here alone
  // FIXME !!! - should not have LeapMotion defined here at all - it should be
  // pub/sub !!!
  public void startLeapTracking() throws Exception {
    if (leap == null) {
      leap = (LeapMotion) startPeer("leap");
    }
    this.index.map(90.0, 0.0, this.index.getMin(), this.index.getMax());
    this.thumb.map(90.0, 50.0, this.thumb.getMin(), this.thumb.getMax());
    this.majeure.map(90.0, 0.0, this.majeure.getMin(), this.majeure.getMax());
    this.ringFinger.map(90.0, 0.0, this.ringFinger.getMin(), this.ringFinger.getMax());
    this.pinky.map(90.0, 0.0, this.pinky.getMin(), this.pinky.getMax());
    leap.addLeapDataListener(this);
    leap.startTracking();
    return;
  }

  public void stop() {
    thumb.stop();
    index.stop();
    majeure.stop();
    ringFinger.stop();
    pinky.stop();
    wrist.stop();
  }

  // FIXME !!! - should not have LeapMotion defined here at all - it should be
  // pub/sub !!!
  public void stopLeapTracking() {
    leap.stopTracking();
    this.index.map(this.index.getMin(), this.index.getMax(), this.index.getMin(), this.index.getMax());
    this.thumb.map(this.thumb.getMin(), this.thumb.getMax(), this.thumb.getMin(), this.thumb.getMax());
    this.majeure.map(this.majeure.getMin(), this.majeure.getMax(), this.majeure.getMin(), this.majeure.getMax());
    this.ringFinger.map(this.ringFinger.getMin(), this.ringFinger.getMax(), this.ringFinger.getMin(), this.ringFinger.getMax());
    this.pinky.map(this.pinky.getMin(), this.pinky.getMax(), this.pinky.getMin(), this.pinky.getMax());
    this.rest();
    return;
  }

  public void test() {
    thumb.moveTo(thumb.getCurrentInputPos() + 2);
    index.moveTo(index.getCurrentInputPos() + 2);
    majeure.moveTo(majeure.getCurrentInputPos() + 2);
    ringFinger.moveTo(ringFinger.getCurrentInputPos() + 2);
    pinky.moveTo(pinky.getCurrentInputPos() + 2);
    wrist.moveTo(wrist.getCurrentInputPos() + 2);

    info("test completed");
  }

  public void three() {
    moveTo(150.0, 0.0, 0.0, 0.0, 180.0, 90.0);
  }

  public void thumbsUp() {
    moveTo(0.0, 180.0, 180.0, 180.0, 180.0, 90.0);
  }

  public void two() {
    victory();
  }

  public void victory() {
    moveTo(150.0, 0.0, 0.0, 180.0, 180.0, 90.0);
  }

  public void waitTargetPos() {
    thumb.waitTargetPos();
    index.waitTargetPos();
    majeure.waitTargetPos();
    ringFinger.waitTargetPos();
    pinky.waitTargetPos();
    wrist.waitTargetPos();
  }

  @Override
  public void detach(String controllerName) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isAttached(String name) {
    return controller != null && name.equals(controller.getName());
  }

  @Override
  public Set<String> getAttached() {
    Set<String> ret = new HashSet<String>();
    if (controller != null) {
      ret.add(controller.getName());
    }
    return ret;
  }

}
