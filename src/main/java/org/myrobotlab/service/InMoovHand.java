package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.LeapData;
import org.myrobotlab.service.data.LeapHand;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.LeapDataListener;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinArrayPublisher;
import org.myrobotlab.service.interfaces.PortConnector;
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
public class InMoovHand extends Service implements LeapDataListener, PinArrayListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovHand.class);

  // default pins if not specified.
  private static final Integer DEFAULT_THUMB_PIN = 2;
  private static final Integer DEFAULT_INDEX_PIN = 3;
  private static final Integer DEFAULT_MAJEURE_PIN = 4;
  private static final Integer DEFAULT_RINGFINGER_PIN = 5;
  private static final Integer DEFAULT_PINKY_PIN = 6;
  private static final Integer DEFAULT_WRIST_PIN = 7;
  
  /**
   * peer services
   */
  transient public LeapMotion leap;
  transient public ServoControl thumb;
  transient public ServoControl index;
  transient public ServoControl majeure;
  transient public ServoControl ringFinger;
  transient public ServoControl pinky;
  transient public ServoControl wrist;
  transient public ServoController controller;
  private String side;
  
  // The pins for the finger tip sensors
  public String[] sensorPins = new String[]{"A0","A1","A2","A3","A4"};
  public int[] sensorThresholds = new int[] {500,500,500,500,500}; 
  // public int[] sensorLastValues = new int[] {0,0,0,0,0};
  public boolean sensorsEnabled = false;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      InMoov i01 = (InMoov) Runtime.start("i01", "InMoov");
      i01.startRightHand("COM15");

      ServoController controller = (ServoController) Runtime.getService("i01.right");
      // arduino.pinMode(13, ServoController.OUTPUT);
      // arduino.digitalWrite(13, 1);

      InMoovHand rightHand = (InMoovHand)Runtime.start("r01", "InMoovHand");//InMoovHand("r01");
      Runtime.createAndStart("gui", "SwingGui");
      rightHand.connect("COM15");
      rightHand.startService();
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
  
  public void releaseService() {
    try {
      disable();
      releasePeers();
      super.releaseService(); 
    } catch (Exception e) {
      error(e);
    }
  }


  public InMoovHand(String n, String id) {
    super(n, id);
  }

  /*
   * attach all the servos - this must be re-entrant and accomplish the
   * re-attachment when servos are detached
   */
  @Deprecated
  public boolean attach() {
    log.warn("attach deprecated please use enable");
    return enable();
  }

  public boolean enable() {
    sleep(InMoov.attachPauseMs);
    thumb.enable();
    sleep(InMoov.attachPauseMs);
    index.enable();
    sleep(InMoov.attachPauseMs);
    majeure.enable();
    sleep(InMoov.attachPauseMs);
    ringFinger.enable();
    sleep(InMoov.attachPauseMs);
    pinky.enable();
    sleep(InMoov.attachPauseMs);
    wrist.enable();
    return true;
  }

  public void bird() {
    moveTo(150.0, 180.0, 0.0, 180.0, 180.0, 90.0);
  }

  @Override
  public void broadcastState() {
    thumb.broadcastState();
    index.broadcastState();
    majeure.broadcastState();
    ringFinger.broadcastState();
    pinky.broadcastState();
    wrist.broadcastState();
  }

  public void close() {
    moveTo(130, 180, 180, 180, 180);
  }

  public void closePinch() {
    moveTo(130, 140, 180, 180, 180);
  }

  // FIXME FIXME - this method must be called
  // user data needed
  /**
   * connect - user data needed
   * 
   * @param port
   *          com port
   * @return true or false
   * @throws Exception
   *           e
   * 
   */
  public boolean connect(String port) throws Exception {

    // justin case we haven't started our peers yet.
    startPeers();
    controller = (ServoController)startPeer("arduino");
    
    if (controller == null) {
      error("controller is invalid");
      return false;
    }

    if (controller instanceof PortConnector) {
      PortConnector arduino = (PortConnector) controller;
      arduino.connect(port);

      if (!arduino.isConnected()) {
        error("controller for %s on port %s not connected", getName(), port);
        return false;
      }
    }
    
    // set defaults for the servos
    initServoDefaults();
    
    // TODO: initSensorPin defaults.
    
    thumb.attach(controller);
    index.attach(controller);
    majeure.attach(controller);
    ringFinger.attach(controller);
    pinky.attach(controller);
    wrist.attach(controller);

    enableAutoEnable(true);

    broadcastState();
    return true;
  }

  private void initServoDefaults() {
    if (thumb.getPin() == null)
      thumb.setPin(DEFAULT_THUMB_PIN);
    if (index.getPin() == null)
      index.setPin(DEFAULT_INDEX_PIN);
    if (majeure.getPin() == null)
      majeure.setPin(DEFAULT_MAJEURE_PIN);
    if (ringFinger.getPin() == null)
      ringFinger.setPin(DEFAULT_RINGFINGER_PIN);
    if (pinky.getPin() == null)
      pinky.setPin(DEFAULT_PINKY_PIN);
    if (wrist.getPin() == null)
        wrist.setPin(DEFAULT_WRIST_PIN);    
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
    
    setVelocity(45.0, 45.0, 45.0, 45.0, 45.0, 45.0);
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

  @Deprecated
  public void detach() {
    log.warn("detach deprecated please use disable");
    disable();

  }

  public void disable() {
    if (thumb != null) {
      thumb.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (index != null) {
      index.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (majeure != null) {
      majeure.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (ringFinger != null) {
      ringFinger.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (pinky != null) {
      pinky.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (wrist != null) {
      wrist.disable();
    }

  }

  @Deprecated
  public void enableAutoEnable(Boolean param) {
  }

  @Deprecated
  public void enableAutoDisable(Boolean param) {
    setAutoDisable(param);
  }

  public void setAutoDisable(Boolean param) {
    thumb.setAutoDisable(param);
    index.setAutoDisable(param);
    majeure.setAutoDisable(param);
    ringFinger.setAutoDisable(param);
    pinky.setAutoDisable(param);
    wrist.setAutoDisable(param);
  }

  public void devilHorns() {
    moveTo(150.0, 0.0, 180.0, 180.0, 0.0, 90.0);
  }

  public void five() {
    open();
  }

  public void four() {
    moveTo(150.0, 0.0, 0.0, 0.0, 0.0, 90.0);
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

  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveHand(\"%s\",%.2f,%.2f,%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, side, thumb.getPos(), index.getPos(), majeure.getPos(),
        ringFinger.getPos(), pinky.getPos(), wrist.getPos());
  }

  public String getSide() {
    return side;
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
    if (thumb != null)
      this.thumb.moveTo(thumb);
    if (index != null)
      this.index.moveTo(index);
    if (majeure != null)
      this.majeure.moveTo(majeure);
    if (ringFinger != null)
      this.ringFinger.moveTo(ringFinger);
    if (pinky != null)
      this.pinky.moveTo(pinky);
    if (wrist != null)
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

  public void waitTargetPos() {
    thumb.waitTargetPos();
    index.waitTargetPos();
    majeure.waitTargetPos();
    ringFinger.waitTargetPos();
    pinky.waitTargetPos();
    wrist.waitTargetPos();
  }

  public void ok() {
    moveTo(150.0, 180.0, 0.0, 0.0, 0.0, 90.0);
  }

  public void one() {
    moveTo(150.0, 0.0, 180.0, 180.0, 180.0, 90.0);
  }

  @Override
  public LeapData onLeapData(LeapData data) {

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

  public void open() {
    rest();
  }

  public void openPinch() {
    moveTo(0, 0, 180, 180, 180);
  }

  // ----- initialization end --------
  // ----- movements begin -----------

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

  public void setPins(int thumbPin, int indexPin, int majeurePin, int ringFingerPin, int pinkyPin, int wristPin) {
    log.info("setPins {} {} {} {} {} {}", thumbPin, indexPin, majeurePin, ringFingerPin, pinkyPin, wristPin);
    try {
      controller.attach(thumb, thumbPin);
      controller.attach(index, indexPin);
      controller.attach(majeure, majeurePin);
      controller.attach(ringFinger, ringFingerPin);
      controller.attach(pinky, pinkyPin);
      controller.attach(wrist, wristPin);
    } catch (Exception e) {
      error(e);
    }
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

  public void setSide(String side) {
    this.side = side;
  }

  @Deprecated
  public void setSpeed(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    log.warn("setspeed deprecated please use setvelocity");
    this.thumb.setSpeed(thumb);
    this.index.setSpeed(index);
    this.majeure.setSpeed(majeure);
    this.ringFinger.setSpeed(ringFinger);
    this.pinky.setSpeed(pinky);
    this.wrist.setSpeed(wrist);
  }

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
    // Handled lazily on connect method now.
    // TODO:
    //    if (controller == null) {
    //      controller = (ServoController) startPeer("arduino");
    //    }
    
    if (thumb == null) {
      thumb = (ServoControl)startPeer("thumb");
    }
    if (index == null) {
      index = (ServoControl)startPeer("index");
    }
    if (majeure == null) {
      majeure = (ServoControl)startPeer("majeure");
    }
    if (ringFinger == null) {
      ringFinger = (ServoControl)startPeer("ringFinger");
    }
    if (pinky == null) {
      pinky = (ServoControl)startPeer("pinky");
    }
    if (wrist == null) {
      wrist = (ServoControl)startPeer("wrist");
    }
    /*
    if (controller == null) {
      controller = (ServoController) createPeer("arduino");
    }
    */
  }

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

    if (controller == null) {
      error("arduino is null");
    }

    if (controller instanceof PortConnector) {
      PortConnector arduino = (PortConnector) controller;
      if (!arduino.isConnected()) {
        error("arduino not connected");
      }
    }

    thumb.moveTo(thumb.getPos() + 2);
    index.moveTo(index.getPos() + 2);
    majeure.moveTo(majeure.getPos() + 2);
    ringFinger.moveTo(ringFinger.getPos() + 2);
    pinky.moveTo(pinky.getPos() + 2);
    wrist.moveTo(wrist.getPos() + 2);

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

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoovHand.class.getCanonicalName());
    meta.addDescription("an easier way to create gestures for InMoov");
    meta.addCategory("robot");

    meta.addPeer("thumb", "Servo", "Thumb servo");
    meta.addPeer("index", "Servo", "Index servo");
    meta.addPeer("majeure", "Servo", "Majeure servo");
    meta.addPeer("ringFinger", "Servo", "RingFinger servo");
    meta.addPeer("pinky", "Servo", "Pinky servo");
    meta.addPeer("wrist", "Servo", "Wrist servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");
    meta.addPeer("leap", "LeapMotion", "Leap Motion Service", false);

    return meta;
  }

  public void setVelocity(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    if (thumb != null)
      this.thumb.setSpeed(thumb);
    if (index != null)
      this.index.setSpeed(index);
    if (majeure != null)
      this.majeure.setSpeed(majeure);
    if (ringFinger != null)
      this.ringFinger.setSpeed(ringFinger);
    if (pinky != null)
      this.pinky.setSpeed(pinky);
    if (wrist != null)
      this.wrist.setSpeed(wrist);
  }

  public void setController(ServoController servoController) {
    this.controller = servoController;
  }

  @Override
  public void onPinArray(PinData[] pindata) {
    
    log.info("On Pin Data: {}", pindata.length);
    if (!sensorsEnabled)
      return;
      // just return ?  TOOD: maybe still track the last read values...
    // TODO : change the interface to get a map of pin data, keyed off the name. ?
    for (PinData pin : pindata) {
      log.info("Pin Data: {}", pin);
      // p
      //      if (sensorPins.contains(pin.pin)) {
      //        // it's one of our finger pins.. let's operate on it.
      //        log.info("Pin Data : {} value {}", pin.pin, pin.value );
      //        if (sensorPins[0].equalsIgnoreCase(pin.pin)) {
      //          // thumb / A0
      //          // here we want to test the pin state.. and potentially take an action 
      //          // based on the updated sensor pin state
      //          if (pin.value > sensorThresholds[0])
      //            thumb.stop();
      //        } else if (sensorPins[1].equalsIgnoreCase(pin.pin)) {
      //          // index / A1
      //          if (pin.value > sensorThresholds[1])
      //            index.stop();
      //
      //        } else if (sensorPins[2].equalsIgnoreCase(pin.pin)) {
      //          // middle / A2
      //          if (pin.value > sensorThresholds[2])
      //            majeure.stop();
      //
      //        } else if (sensorPins[3].equalsIgnoreCase(pin.pin)) {
      //          // ring / A3
      //          if (pin.value > sensorThresholds[3])
      //            ringFinger.stop();
      //
      //        } else if (sensorPins[4].equalsIgnoreCase(pin.pin)) {
      //          // pinky / A4
      //          if (pin.value > sensorThresholds[4])
      //            pinky.stop();
      //        }
      //      }
    }
  }


  /**
   * this method returns the analog pins that the hand is listening to.
   * The InMoovHand listens on analog pins A0-A4 for the finger tip sensors.
   * 
   */
  @Override
  public String[] getActivePins() {
    // TODO Auto-generated method stub
    // for the InMoov hand, we're just going to say A0 - A4 ... for now..
    return sensorPins;
  }

  /**
   * Set the array of pins that should be listened to. 
   * 
   * @param pins
   */
  public void setSensorPins(String[] pins) {
    // TODO, this should probably be a sorted set.. and sensorPins itself should probably be a map to keep the mapping of pin to finger 
    this.sensorPins = pins;
  }

}
