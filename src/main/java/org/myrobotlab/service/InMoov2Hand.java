package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.InMoov2HandConfig;
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
public class InMoov2Hand extends Service<InMoov2HandConfig> implements LeapDataListener, PinArrayListener {

  public final static Logger log = LoggerFactory.getLogger(InMoov2Hand.class);

  private static final long serialVersionUID = 1L;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");
      i01.startPeer("rightHand");

      boolean done = true;
      if (done) {
        return;
      }

      ServoController controller = (ServoController) Runtime.getService("i01.right");

      InMoov2Hand rightHand = (InMoov2Hand) Runtime.start("r01", "InMoov2Hand");// InMoovHand("r01");
      rightHand.close();
      rightHand.open();
      rightHand.openPinch();
      rightHand.closePinch();
      rightHand.rest();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  transient public ServoController controller;
  public String controllerName;
  /**
   * list of names of possible controllers
   */
  public List<String> controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
  transient public ServoControl index;
  boolean isAttached = false;
  /**
   * peer services FIXME - need to be protected !
   */
  transient public LeapMotion leap;
  transient public ServoControl majeure;

  transient public ServoControl pinky;
  transient public ServoControl ringFinger;
  // The pins for the finger tip sensors
  public String[] sensorPins = new String[] { "A0", "A1", "A2", "A3", "A4" };

  // public int[] sensorLastValues = new int[] {0,0,0,0,0};
  public boolean sensorsEnabled = false;
  public int[] sensorThresholds = new int[] { 500, 500, 500, 500, 500 };

  transient public ServoControl thumb;

  transient public ServoControl wrist;

  public InMoov2Hand(String n, String id) {
    super(n, id);
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

  public void attach(String controllerName, int sensorPin) throws Exception {
    attach((ServoController) Runtime.getService(controllerName), sensorPin);
  }

  public void attach(String controllerName, String sensorPin) throws Exception {
    attach((ServoController) Runtime.getService(controllerName), Integer.parseInt(sensorPin));
  }

  public void bird() {
    moveTo(150.0, 180.0, 0.0, 180.0, 180.0, 90.0);
  }

  @Override
  public Service broadcastState() {
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
    return this;
  }

  public void close() {
    moveTo(130, 180, 180, 180, 180);
  }

  public void closePinch() {
    moveTo(130, 140, 180, 180, 180);
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

  public void devilHorns() {
    moveTo(150.0, 0.0, 180.0, 180.0, 0.0, 90.0);
  }

  public void disable() {
    if (thumb != null)
      thumb.disable();
    if (index != null)
      index.disable();
    if (majeure != null)
      majeure.disable();
    if (ringFinger != null)
      ringFinger.disable();
    if (pinky != null)
      pinky.disable();
    if (wrist != null)
      wrist.disable();
  }

  public boolean enable() {
    if (thumb != null)
      thumb.enable();
    if (index != null)
      index.enable();
    if (majeure != null)
      majeure.enable();
    if (ringFinger != null)
      ringFinger.enable();
    if (pinky != null)
      pinky.enable();
    if (wrist != null)
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
    if (thumb != null)
      thumb.fullSpeed();
    if (index != null)
      index.fullSpeed();
    if (majeure != null)
      majeure.fullSpeed();
    if (ringFinger != null)
      ringFinger.fullSpeed();
    if (pinky != null)
      pinky.fullSpeed();
    if (wrist != null)
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

  @Override
  public Set<String> getAttached() {
    Set<String> ret = new HashSet<String>();
    if (controller != null) {
      ret.add(controller.getName());
    }
    return ret;
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

  public long getLastActivityTime() {

    long lastActivityTime = Math.max(index.getLastActivityTime(), thumb.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, index.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, majeure.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, ringFinger.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, pinky.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, wrist.getLastActivityTime());

    return lastActivityTime;

  }

  public String getScript(String service) {
    String side = getName().contains("left") ? "left" : "right";
    return String.format("%s.moveHand(\"%s\",%.0f,%.0f,%.0f,%.0f,%.0f,%.0f)\n", service, side, thumb.getCurrentInputPos(), index.getCurrentInputPos(), majeure.getCurrentInputPos(),
        ringFinger.getCurrentInputPos(), pinky.getCurrentInputPos(), wrist.getCurrentInputPos());
  }

  public void hangTen() {
    moveTo(0.0, 180.0, 180.0, 180.0, 0.0, 90.0);
  }

  public boolean isAttached() {
    if (controller != null) {
      if (((Arduino) controller).getDeviceId(this) != null) {
        isAttached = true;
        return true;
      }
      controller = null;
    }
    isAttached = false;
    return false;
  }

  @Override
  public boolean isAttached(String name) {
    return controller != null && name.equals(controller.getName());
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

  public void map(double minX, double maxX, double minY, double maxY) {
    if (thumb != null) {
      thumb.map(minX, maxX, minY, maxY);
    }
    if (index != null) {
      index.map(minX, maxX, minY, maxY);
    }
    if (majeure != null) {
      majeure.map(minX, maxX, minY, maxY);
    }
    if (ringFinger != null) {
      ringFinger.map(minX, maxX, minY, maxY);
    }
    if (pinky != null) {
      pinky.map(minX, maxX, minY, maxY);
    }
  }

  // TODO - waving thread fun
  public void moveTo(double thumb, double index, double majeure, double ringFinger, double pinky) {
    moveTo(thumb, index, majeure, ringFinger, pinky, null);
  }

  public void moveTo(Double thumbPos, Double indexPos, Double majeurePos, Double ringFingerPos, Double pinkyPos, Double wristPos) {
    if (log.isDebugEnabled()) {
      log.debug("{}.moveTo {} {} {} {} {} {}", getName(), thumbPos, indexPos, majeurePos, ringFingerPos, pinkyPos, wristPos);
    }
    if (thumb != null && thumbPos != null) {
      thumb.moveTo(thumbPos);
    }
    if (index != null && indexPos != null) {
      index.moveTo(indexPos);
    }
    if (majeure != null && majeurePos != null) {
      majeure.moveTo(majeurePos);
    }
    if (ringFinger != null && ringFingerPos != null) {
      ringFinger.moveTo(ringFingerPos);
    }
    if (pinky != null && pinkyPos != null) {
      pinky.moveTo(pinkyPos);
    }
    if (wrist != null && wristPos != null) {
      wrist.moveTo(wristPos);
    }
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

  @Override
  public LeapData onLeapData(LeapData data) {
    String side = getName().contains("left") ? "left" : "right";
    if (!data.frame.isValid()) {
      // TODO: we could return void here? not sure
      // who wants the return value form this method.
      log.debug("Leap data frame not valid.");
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

    // If the hand data came from a valid frame, update the finger postions.
    // move all fingers
    if (h != null) {
      if (index != null) {
        index.moveTo(h.index);
      } else {
        log.debug("Index finger isn't attached or is null.");
      }
      if (thumb != null) {
        thumb.moveTo(h.thumb);
      } else {
        log.debug("Thumb isn't attached or is null.");
      }
      if (pinky != null) {
        pinky.moveTo(h.pinky);
      } else {
        log.debug("Pinky finger isn't attached or is null.");
      }
      if (ringFinger != null) {
        ringFinger.moveTo(h.ring);
      } else {
        log.debug("Ring finger isn't attached or is null.");
      }
      if (majeure != null) {
        majeure.moveTo(h.middle);
      } else {
        log.debug("Middle(Majeure) finger isn't attached or is null.");
      }
    }

    return data;
  }

  @Deprecated /* use onMove(map) */
  public void onMoveHand(HashMap<String, Double> map) {
    onMove(map);
  }

  public void onMove(Map<String, Double> map) {
    moveTo(map.get("thumb"), map.get("index"), map.get("majeure"), map.get("ringFinger"), map.get("pinky"), map.get("wrist"));
  }

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

  public void onRegistered(Registration s) {
    refreshControllers();
    broadcastState();
  }

  public void open() {
    rest();
  }

  public void openPinch() {
    moveTo(0, 0, 180, 180, 180);
  }

  public void refresh() {
    broadcastState();
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
    return controllers;
  }

  public void release() {
    disable();
  }

  @Override
  public void releaseService() {
    try {
      disable();
      super.releaseService();
    } catch (Exception e) {
      error(e);
    }
  }

  public void rest() {
    if (thumb != null)
      thumb.rest();
    if (index != null)
      index.rest();
    if (majeure != null)
      majeure.rest();
    if (ringFinger != null)
      ringFinger.rest();
    if (pinky != null)
      pinky.rest();
    if (wrist != null)
      wrist.rest();
  }

  @Override
  public boolean save() {
    super.save();
    if (thumb != null)
      thumb.save();
    if (index != null)
      index.save();
    if (majeure != null)
      majeure.save();
    if (ringFinger != null)
      ringFinger.save();
    if (pinky != null)
      pinky.save();
    if (wrist != null)
      wrist.save();
    return true;
  }

  public void setAutoDisable(Boolean param) {
    if (thumb != null)
      thumb.setAutoDisable(param);
    if (index != null)
      index.setAutoDisable(param);
    if (majeure != null)
      majeure.setAutoDisable(param);
    if (ringFinger != null)
      ringFinger.setAutoDisable(param);
    if (pinky != null)
      pinky.setAutoDisable(param);
    if (wrist != null)
      wrist.setAutoDisable(param);
  }

  public void setPins(int thumbPin, int indexPin, int majeurePin, int ringFingerPin, int pinkyPin, int wristPin) {
    log.info("setPins {} {} {} {} {} {}", thumbPin, indexPin, majeurePin, ringFingerPin, pinkyPin, wristPin);
    if (thumb != null)
      thumb.setPin(thumbPin);
    if (index != null)
      index.setPin(indexPin);
    if (majeure != null)
      majeure.setPin(majeurePin);
    if (ringFinger != null)
      ringFinger.setPin(ringFingerPin);
    if (pinky != null)
      pinky.setPin(pinkyPin);
    if (wrist != null)
      wrist.setPin(wristPin);
  }

  public void setRest(double thumb, double index, double majeure, double ringFinger, double pinky) {
    setRest(thumb, index, majeure, ringFinger, pinky, null);
  }

  public void setRest(double thumbRest, double indexRest, double majeureRest, double ringFingerRest, double pinkyRest, Double wristRest) {
    log.info("setRest {} {} {} {} {} {}", thumb, index, majeure, ringFinger, pinky, wrist);
    if (thumb != null)
      thumb.setRest(thumbRest);
    if (index != null)
      index.setRest(indexRest);
    if (majeure != null)
      majeure.setRest(majeureRest);
    if (ringFinger != null)
      ringFinger.setRest(ringFingerRest);
    if (pinky != null)
      pinky.setRest(pinkyRest);
    if (wrist != null) {
      wrist.setRest(wristRest);
    }
  }

  /**
   * @param pins
   *          Set the array of pins that should be listened to.
   * 
   */
  public void setSensorPins(String[] pins) {
    // TODO, this should probably be a sorted set.. and sensorPins itself should
    // probably be a map to keep the mapping of pin to finger
    this.sensorPins = pins;
  }

  public void setSpeed(Double thumbSpeed, Double indexSpeed, Double majeureSpeed, Double ringFingerSpeed, Double pinkySpeed, Double wristSpeed) {

    if (thumb != null)
      thumb.setSpeed(thumbSpeed);
    if (index != null)
      index.setSpeed(indexSpeed);
    if (majeure != null)
      majeure.setSpeed(majeureSpeed);
    if (ringFinger != null)
      ringFinger.setSpeed(ringFingerSpeed);
    if (pinky != null)
      pinky.setSpeed(pinkySpeed);
    if (wrist != null)
      wrist.setSpeed(wristSpeed);
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

  @Override
  public void startService() {
    super.startService();
    thumb = (ServoControl) startPeer("thumb");
    index = (ServoControl) startPeer("index");
    majeure = (ServoControl) startPeer("majeure");
    ringFinger = (ServoControl) startPeer("ringFinger");
    pinky = (ServoControl) startPeer("pinky");
    wrist = (ServoControl) startPeer("wrist");
  }

  public void stop() {
    if (thumb != null)
      thumb.stop();
    if (index != null)
      index.stop();
    if (majeure != null)
      majeure.stop();
    if (ringFinger != null)
      ringFinger.stop();
    if (pinky != null)
      pinky.stop();
    if (wrist != null)
      wrist.stop();
  }

  // FIXME !!! - should not have LeapMotion defined here at all - it should be
  // pub/sub !!!
  public void stopLeapTracking() {
    leap.stopTracking();
    index.map(index.getMin(), index.getMax(), index.getMin(), index.getMax());
    thumb.map(thumb.getMin(), thumb.getMax(), thumb.getMin(), thumb.getMax());
    majeure.map(majeure.getMin(), majeure.getMax(), majeure.getMin(), majeure.getMax());
    ringFinger.map(ringFinger.getMin(), ringFinger.getMax(), ringFinger.getMin(), ringFinger.getMax());
    pinky.map(pinky.getMin(), pinky.getMax(), pinky.getMin(), pinky.getMax());
    rest();
    return;
  }

  public void test() {
    if (thumb != null)
      thumb.moveTo(thumb.getCurrentInputPos() + 2);
    if (index != null)
      index.moveTo(index.getCurrentInputPos() + 2);
    if (majeure != null)
      majeure.moveTo(majeure.getCurrentInputPos() + 2);
    if (ringFinger != null)
      ringFinger.moveTo(ringFinger.getCurrentInputPos() + 2);
    if (pinky != null)
      pinky.moveTo(pinky.getCurrentInputPos() + 2);
    if (wrist != null)
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
    if (thumb != null)
      thumb.waitTargetPos();
    if (index != null)
      index.waitTargetPos();
    if (majeure != null)
      majeure.waitTargetPos();
    if (ringFinger != null)
      ringFinger.waitTargetPos();
    if (pinky != null)
      pinky.waitTargetPos();
    if (wrist != null)
      wrist.waitTargetPos();
  }
}
