package org.myrobotlab.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.RangeListener;
import org.myrobotlab.service.interfaces.RangePublisher;
import org.myrobotlab.service.interfaces.UltrasonicSensorControl;
import org.myrobotlab.service.interfaces.UltrasonicSensorController;
import org.slf4j.Logger;

/**
 * 
 * UltrasonicSensor - This will read data from an ultrasonic sensor module
 * connected to an android.
 * 
 * A device which uses the UltrasonicSensor would implement RangeListener.
 * UltrasonicSensor implements RangeListener just for testing purposes
 *
 */
public class UltrasonicSensor extends Service implements RangeListener, RangePublisher, UltrasonicSensorControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(UltrasonicSensor.class);

  public final Set<String> types = new HashSet<String>(Arrays.asList("SR04", "SR05"));
  private int pings;

  // currently not variable in NewPing.h
  // Integer maxDistanceCm = 500;

  private Integer trigPin = null;
  private Integer echoPin = null;
  private String type = "SR04";

  private Double lastRaw;
  private Double lastRange;

  // for blocking asynchronous data
  private boolean isBlocking = false;

  transient private BlockingQueue<Double> data = new LinkedBlockingQueue<Double>();

  private transient UltrasonicSensorController controller;

  String controllerName;

  double multiplier = 1;

  double offset = 0;

  long timeout = 500;

  public UltrasonicSensor(String n) {
    super(n);
  }

  // ---- part of interfaces begin -----

  // Uber good - .. although this is "chained" versus star routing
  // Star routing would be routing from the Arduino directly to the Listener
  // The "chained" version takes 2 thread contexts :( .. but it has the
  // benefit
  // of the "publishRange" method being affected by the Sensor service e.g.
  // change units, sample rate, etc
  // FIXME - NOT SERVICE .. possibly name or interface but not service
  public void addRangeListener(Service service) {
    addListener("publishRange", service.getName(), "onRange");
  }

  public void attach(String port, int trigPin, int echoPin) throws Exception {
    UltrasonicSensorController peerController = null;
    // prepare the peer
    if (controller != null) {
      peerController = controller;
    } else {
      peerController = (UltrasonicSensorController) startPeer("controller");
    }
    // connect it
    peerController.connect(port);
    // attach it
    attach(peerController, trigPin, echoPin);
    controller = peerController;
  }

  public void attach(UltrasonicSensorController controller, Integer trigPin, Integer echoPin) throws Exception {

    // critical test
    // refer to http://myrobotlab.org/content/control-controller-manifesto
    if (isAttached(controller)) {
      return;
    }

    if (this.controller != null) {
      log.info("controller already attached - detach {} before attaching {}", this.controller.getName(), controller.getName());
    }

    // critical init code
    this.trigPin = trigPin;
    this.echoPin = echoPin;
    this.controller = controller;
    this.controllerName = controller.getName();

    // call other service's attach
    controller.attach(this, trigPin, echoPin);

  }

  private boolean isAttached(UltrasonicSensorController controller) {
    return this.controller == controller;
  }

  // FIXME - should be MicroController Interface ..
  public UltrasonicSensorController getController() {
    return controller;
  }

  public int getEchoPin() {
    return echoPin;
  }

  public int getTriggerPin() {
    return trigPin;
  }

  @Override
  public void onRange(Double range) {
    log.info(String.format("RANGE: %d", range));
  }

  public Double publishRange(Double range) {

    ++pings;

    lastRange = range; // * 0.393701 inches

    log.info("publishRange {}", lastRange);
    return lastRange;
  }

  public boolean setType(String type) {
    if (types.contains(type)) {
      this.type = type;
      return true;
    }
    return false;
  }

  // ---- part of interfaces end -----

  @Override
  public void startRanging() {
    controller.ultrasonicSensorStartRanging(this);
  }

  @Override
  public void stopRanging() {
    controller.ultrasonicSensorStopRanging(this);
  }

  synchronized public Double range() {
    Double rawMs = ping();
    if (rawMs == null) {
      return null;
    }
    return rawMs * multiplier + offset;
  }

  synchronized public Double ping() {
    data.clear();
    startRanging();
    isBlocking = true;

    try {
      return data.poll(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
    } finally {
      isBlocking = false;
      stopRanging();
    }

    error("timeout of %d reached", timeout);
    return null;
  }

  // probably should do this in a util class
  public static int byteArrayToInt(int[] b) {
    return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
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

    ServiceType meta = new ServiceType(UltrasonicSensor.class.getCanonicalName());
    meta.addDescription("ranging sensor");
    meta.addCategory("sensor");
    meta.addPeer("controller", "Arduino", "default sensor controller will be an Arduino");
    return meta;
  }

  public int getPings() {
    return pings;
  }

  public String getType() {
    return type;
  }

  // TODO - this could be Java 8 default interface implementation
  @Override
  public void detach(String controllerName) {
    if (controller == null || !controllerName.equals(controller.getName())) {
      return;
    }
    controller.detach(this);
    controller = null;
  }

  @Override
  public Double onUltrasonicSensorData(Double rawMs) {
    // data comes in 'raw' and leaves as Range
    // TODO implement changes based on type of sensor SRF04 vs SRF05
    // TODO implement units preferred
    // direct callback vs pub/sub (this needs to be handled by the
    // framework)

    // FIXME - convert to appropriate range
    // inches/meters/other kubits?

    ++pings;
    lastRaw = rawMs;
    Double range = (rawMs * multiplier + offset);
    if (isBlocking) {
      try {
        data.put(lastRaw);
      } catch (InterruptedException e) {
        Logging.logError(e);
      }
    }

    invoke("publishRange", range);
    return range;
  }

  @Override
  public void setUnitCm() {
    multiplier = 1;
  }

  @Override
  public void setUnitInches() {
    multiplier = 0.393701;
  }

  @Override
  public boolean isAttached(String name) {
    return controller != null;
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null) {
      ret.add(controller.getName());
    }
    return ret;
  }

  public static void main(String[] args) {
    LoggingFactory.init("INFO");

    try {

      VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
      UltrasonicSensor srf04 = (UltrasonicSensor) Runtime.start("srf04", "UltrasonicSensor");
      // Runtime.start("python", "Python");
      Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");

      int trigPin = 8;
      int echoPin = 7;

      // TODO test with externally supplied arduino
      // virtual.connect("COM10");

      srf04.attach("COM5", trigPin, echoPin);

      Arduino arduino = (Arduino) srf04.getController();
      // arduino.enableBoardInfo(true);
      // arduino.enableBoardInfo(false);
      // arduino.setDebug(false);

      Servo servo = (Servo) Runtime.start("servo", "Servo");
      servo.attach(arduino, 6);
      servo.moveTo(30);

      srf04.startRanging();

      for (int i = 0; i < 100; ++i) {
        servo.moveTo(30);
        servo.moveTo(160);
        servo.moveTo(10);
        servo.moveTo(180);
      }

      arduino.setDebug(false);

      srf04.stopRanging();

      arduino.setDebug(false);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}