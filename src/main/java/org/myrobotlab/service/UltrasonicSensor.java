package org.myrobotlab.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.UltrasonicSensorConfig;
import org.myrobotlab.service.data.RangeData;
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
public class UltrasonicSensor extends Service<UltrasonicSensorConfig> implements RangeListener, RangePublisher, UltrasonicSensorControl {

  private final static Logger log = LoggerFactory.getLogger(UltrasonicSensor.class);

  private static final long serialVersionUID = 1L;

  transient protected UltrasonicSensorController controller;

  protected String controllerName;

  transient protected BlockingQueue<Double> data = new LinkedBlockingQueue<Double>();

  @Deprecated /* use directly from config - should be String */
  protected Integer echoPin = null;

  protected boolean isAttached = false;

  // for blocking asynchronous data
  protected boolean isBlocking = false;

  protected boolean isRanging = false;

  protected Double lastRange;

  protected Double max;

  protected Double min;

  protected double multiplier = 1;

  protected double offset = 0;

  protected long pingCount = 0;

  protected Double rateHz = 1.0;

  protected long timeout = 500;

  @Deprecated /* use directly from config - should be String */
  protected Integer trigPin = null;

  protected final Set<String> types = new HashSet<String>(Arrays.asList("SR04", "SR05"));

  protected boolean useRate = false;

  protected long nextSampleTs = System.currentTimeMillis();

  public UltrasonicSensor(String n, String id) {
    super(n, id);
    registerForInterfaceChange(UltrasonicSensorController.class);
  }

  public void addRangeListener(Service service) {
    addListener("publishRange", service.getName(), "onRange");
  }

  @Override
  public void attach(Attachable service) throws Exception {
    if (service instanceof UltrasonicSensorController) {
      attach((UltrasonicSensorController) service, trigPin, echoPin);
      return;
    }
    log.error("do not know how to attach to a {}", service.getClass().getSimpleName());
  }

  @Override
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
    isAttached = true;
    broadcastState();
  }

  /**
   * This method is used to clear the Min, Max, last range and ping count values.
   */
  public void clear() {
    pingCount = 0;
    lastRange = null;
    min = null;
    max = null;
    broadcastState();
  }

  // TODO - this could be Java 8 default interface implementation
  @Override
  public void detach(String controllerName) {
    isAttached = false;
    if (controller == null || !controllerName.equals(controller.getName())) {
      return;
    }
    controller.detach(this);
    controller = null;
    this.controllerName = null;
    broadcastState();
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null) {
      ret.add(controller.getName());
    }
    return ret;
  }

  @Override
  public UltrasonicSensorConfig getConfig() {

    super.getConfig();
    // FIXME - remove member variables use config directly
    config.controller = controllerName;
    config.triggerPin = trigPin;
    config.echoPin = echoPin;
    config.timeout = timeout;

    return config;
  }

  // FIXME - should be MicroController Interface ..
  public UltrasonicSensorController getController() {
    return controller;
  }

  /**
   * There are two pins used on an Ultrasonic sensor, Trigger and Echo.
   * This function returns the Echo Pin which is the pin the signal is returned on.
   * @return
   * Pin number.
   */
  public int getEchoPin() {
    return echoPin;
  }

  /**
   * Used to get the Maximum range detected since the last clear() command.
   * @return
   * centimeters or inches based on the current setting.
   */
  public Double getMax() {
    return max;
  }

  /**
   * Used to get the Minimum range detected since the last clear() command.
   * @return
   * centimeters or inches based on the current setting.
   */
  public Double getMin() {
    return min;
  }

  /**
   * Used to get the number of pings sent since the last clear() command.
   * @return
   * Number of pings.
   */
  public long getPingCount() {
    return pingCount;
  }

  /**
   * There are two pins used on an Ultrasonic sensor, Trigger and Echo.
   * This function returns the Trigger Pin which is used to trigger the ping.
   * @return
   * Pin number.
   */
  public int getTriggerPin() {
    return trigPin;
  }
  
  public boolean isAttached() {
    return isAttached;
  }

  @Override
  public boolean isAttached(String name) {
    return isAttached;
  }

  protected boolean isAttached(UltrasonicSensorController controller) {
    return this.controller == controller;
  }

  @Override
  public UltrasonicSensorConfig apply(UltrasonicSensorConfig c) {
    super.apply(c);

    if (c.triggerPin != null)
      setTriggerPin(c.triggerPin);

    if (c.echoPin != null)
      setEchoPin(c.echoPin);

    if (c.timeout != null)
      timeout = c.timeout;

    if (c.controller != null) {
      try {
        attach(c.controller);
      } catch (Exception e) {
        error(e);
      }
    }

    return c;
  }

  @Override
  public void onRange(Double range) {
    log.info("RANGE: {}", range);
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
    Double range = (rawMs * multiplier + offset);

    if (useRate) {
      // FIXME - average - then publish ?
      if (System.currentTimeMillis() < nextSampleTs) {
        // using rate limiting and not yet ready to process
        return range;
      } else {
        nextSampleTs = System.currentTimeMillis() + (long) (1000 * 1 / rateHz);
      }
    }

    if (isBlocking) {
      try {
        data.put(range);
      } catch (InterruptedException e) {
        // don't care
      }
    }

    if (max == null || range > max) {
      max = range;
    }

    if (min == null || range < min) {
      min = range;
    }

    pingCount++;

    invoke("publishRange", range);
    // range with source
    invoke("publishRangeData", range);
    return range;
  }

  /**
   * The raw time value that came back from the trigger pin after the echo pin
   * was activated
   * 
   * @return - time in micro seconds
   */
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

  @Override
  public Double publishRange(Double range) {

    lastRange = range; // * 0.393701 inches

    log.info("publishRange {}", lastRange);
    return lastRange;
  }

  public RangeData publishRangeData(Double range) {
    RangeData ret = new RangeData(getName(), range);
    return ret;
  }

  /**
   * Takes the value of ping and applies multiplier and offset for the desired
   * value in cm or inches
   * 
   * @return - converted value
   */
  synchronized public Double range() {
    Double rawMs = ping();
    if (rawMs == null) {
      return null;
    }
    return rawMs * multiplier + offset;
  }

  public void setContinuous(boolean b) {
    isBlocking = !b;
    broadcastState();
  }

  /**
   * There are two pins used on an Ultrasonic sensor, Trigger and Echo.
   * This method set the Echo Pin which is the pin the signal is returned on.
   * @param pin
   *  The pin the echo will be returned on.
   */
  public void setEchoPin(int pin) {
    echoPin = pin;
  }

  /**
   * There are two pins used on an Ultrasonic sensor.
   * Trigger and Echo.
   * This method set the Trigger Pin which starts the ranging process.
   * @param pin
   *  The pin used to trigger the ranging process.
   */
  public void setTriggerPin(int pin) {
    trigPin = pin;
  }

  @Override
  public void setUnitCm() {
    multiplier = 1;
  }

  @Override
  public void setUnitInches() {
    multiplier = 0.393701;
  }

  /**
   * Sets the rate at which ranging is done to be as fast as possible (Uses more processing power).
   */
  public void maxRate() {
    useRate = false;
  }

  /**
   * Sets the ranging rate to the rate using setRate(value).
   */
  public void useRate() {
    useRate = true;
  }

  /**
   * Set the ping rate when ranging to the frequence supplied in the parameter.
   * To set rates lower than one, use public void setRate(double hz) instead.
   * @param hz
   */
  public void setRate(int hz) {
    setRate((double) hz);
  }

  /**
   * Set the ping rate when ranging to the frequence supplied in the parameter.
   * @param hz
   */
  public void setRate(double hz) {
    rateHz = hz;
    useRate();
  }

  @Override
  public void startRanging() {
    if (controller == null || !controller.isConnected()) {
      info("controller must be attached and connected to range");      
      return;
    }
    isRanging = true;
    controller.ultrasonicSensorStartRanging(this);
  }

  @Override
  public void stopRanging() {
    isRanging = false;
    if (controller != null) {
      controller.ultrasonicSensorStopRanging(this);
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init("INFO");

    try {

      // Runtime.setAllVirtual(true);
      Runtime.start("webgui", "WebGui");

      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("/dev/ttyACM2");

      UltrasonicSensor srf04 = (UltrasonicSensor) Runtime.start("srf04", "UltrasonicSensor");

      srf04.setTriggerPin(3);
      srf04.setEchoPin(2);

      // TODO test with externally supplied arduino
      // virtual.connect("COM10");

      srf04.attach(arduino);

      boolean done = true;
      if (done) {
        return;
      }

      srf04.attach("arduino");

      // arduino.enableBoardInfo(true);
      // arduino.enableBoardInfo(false);
      // arduino.setDebug(false);

      Servo servo = (Servo) Runtime.start("servo", "Servo");
      servo.setPin(6);
      servo.attach(arduino);
      // servo.attach(arduino, 6);
      servo.moveTo(30.0);

      srf04.startRanging();

      for (int i = 0; i < 100; ++i) {
        servo.moveTo(30.0);
        servo.moveTo(160.0);
        servo.moveTo(10.0);
        servo.moveTo(180.0);
      }

      arduino.setDebug(false);

      srf04.stopRanging();

      arduino.setDebug(false);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}
