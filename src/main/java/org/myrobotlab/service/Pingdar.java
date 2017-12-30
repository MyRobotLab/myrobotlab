package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.RangeListener;
import org.myrobotlab.service.interfaces.RangingControl;
import org.slf4j.Logger;

/**
 * 
 * Pingdar - this service will control a sweeping servo and an ultrasonic sensor
 * module. The result is a sonar style range finding.
 *
 */
public class Pingdar extends Service implements RangingControl, RangeListener {

  public static class Point {

    public double r;
    public double theta;

    public Point(double servoPos, double z) {
      this.theta = servoPos;
      this.r = z;
    }

  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Pingdar.class);

  public int sweepMin = 0;
  public int sweepMax = 180;

  public int step = 1;
  transient private Servo servo;
  transient private UltrasonicSensor sensor;
  
  // TODO - changed to XDar - make RangeSensor interface -> publishRange
  // TODO - set default sample rate
  // private boolean isAttached = false;
  private Double lastRange = 0.0;

  private Double lastPos = 0.0;

  // private int rangeCount = 0;

  // long rangeAvg = 0;

  public Pingdar(String n) {
    super(n);
  }

  // ----------- interface begin ----------------

  public boolean attach(UltrasonicSensor sensor, Servo servo) throws Exception {
    this.sensor = sensor;
    this.servo = servo;

    sensor.addRangeListener(this);
    servo.addServoEventListener(this);
    // from the Arduino and send it back in on packet ..
    return true;
  }

  // ----------- interface end ----------------

  public UltrasonicSensor getSensor() {
    return sensor;
  }

  public Servo getServo() {
    return servo;
  }

  // sensor data has come in
  // grab the latest position
  @Override
  public void onRange(Double range) {
    info("range %d", range.intValue());
    invoke("publishPingdar", new Point(lastPos, range));
    lastRange = range;
  }

  public Double onServoEvent(Double pos) {
    info("pos %d", pos.intValue());
    /*
    lastPos = pos;
    if (rangeCount > 0) {
      Point p = new Point(lastPos, rangeAvg / rangeCount);
      rangeAvg = 0;
      rangeCount = 0;
      invoke("publishPingdar", p);
    }
    */

    invoke("publishPingdar", new Point(pos, lastRange));
    lastPos = pos;
    return lastPos;
  }

  public Point publishPingdar(Point point) {
    return point;
  }

  @Override
  public void startService() {
    super.startService();
    broadcastState();
  }

  public void stop() {
    sensor.stopRanging();
    servo.stop();
  }

  public void sweep() {
    sweep(sweepMin, sweepMax);
  }

  public void sweep(int sweepMin, int sweepMax) {
    this.sweepMin = sweepMin;
    this.sweepMax = sweepMax;
    this.step = 1;

    // TODO - configurable speed
    sensor = getSensor();
    servo = getServo();

    sensor.addRangeListener(this);
    servo.addServoEventListener(this);

    // servo.setSpeed(60);
    servo.setVelocity(30);
    servo.eventsEnabled(true);
    
    sensor.startRanging();
    // STEP ???
    servo.sweep(sweepMin, sweepMax, 100, step);
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

    ServiceType meta = new ServiceType(Pingdar.class.getCanonicalName());
    meta.addDescription("used as a ultra sonic radar");
    meta.addCategory("sensor", "display");
    // put peer definitions in
    meta.addPeer("controller", "Arduino", "controller for servo and sensor");
    meta.addPeer("sensor", "UltrasonicSensor", "sensor");
    meta.addPeer("servo", "Servo", "servo");
    
    meta.sharePeer("sensor.controller", "controller", "Arduino", "shared arduino");
    // theoretically - Servo should follow the same share config
    // meta.sharePeer("servo.controller", "controller", "Arduino", "shared arduino");

    return meta;
  }

  @Override
  public void startRanging() {
    if (sensor != null){
      sensor.startRanging();
    } else {
      error("null sensor");
    }
  }

  @Override
  public void stopRanging() {
    if (sensor != null){
      sensor.stopRanging();
    } else {
      error("null sensor");
    } 
  }
  

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      Runtime.start("gui", "SwingGui");
      
      VirtualArduino virtual = (VirtualArduino)Runtime.start("virtual","VirtualArduino");
      Servo servo = (Servo)Runtime.start("servo","Servo");
      UltrasonicSensor sr04 = (UltrasonicSensor)Runtime.start("sr04", "UltrasonicSensor");
      sleep(1000);
      virtual.connect("COM5");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("COM5");
      sr04.attach(arduino, 12, 11);
      servo.attach(arduino, 2);

      Pingdar pingdar = (Pingdar) Runtime.start("pingdar", "Pingdar");
      sleep(1000);
      pingdar.attach(sr04, servo);
      pingdar.sweep(70, 100);
      sleep(5000);
      pingdar.stop();

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  @Override
  public void setUnitCm() {
    sensor.setUnitCm();
  }

  @Override
  public void setUnitInches() {
    sensor.setUnitInches();
  }

}
