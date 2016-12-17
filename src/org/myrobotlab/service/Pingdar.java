package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * 
 * Pingdar - this service will control a sweeping servo and an ultrasonic sensor
 * module. The result is a sonar style range finding.
 *
 */
public class Pingdar extends Service {

  public static class Point {

    public float r;
    public float theta;

    public Point(float servoPos, float z) {
      this.theta = servoPos;
      this.r = z;
    }

  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Pingdar.class);

  public int sweepMin = 0;
  public int sweepMax = 180;

  public int step = 1;
  transient private Arduino arduino;
  transient private Servo servo;

  transient private UltrasonicSensor sensor;
  // TODO - changed to XDar - make RangeSensor interface -> publishRange
  // TODO - set default sample rate
  // private boolean isAttached = false;
  private Long lastRange;

  private Integer lastPos;

  private int rangeCount = 0;

  long rangeAvg = 0;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    Runtime.createAndStart("gui", "GUIService");
    // Runtime.createAndStart("webgui", "WebGui");
    /*
     * Serial.createNullModemCable("uart", "COM10"); Serial serial =
     * (Serial)Runtime.createAndStart("uart", "Serial");
     * 
     * serial.connect("uart");
     */

    Runtime.start("pingdar", "Pingdar");

    // Runtime.createAndStart("gui", "GUIService");

    // pingdar.attach("COM15", 7, 8, 9);
    // pingdar.sweep();
    /*
     * GUIService gui = new GUIService("gui"); gui.startService();
     */
  }

  public Pingdar(String n) {
    super(n);
  }

  // ----------- interface begin ----------------

  public boolean attach(Arduino arduino, String port, UltrasonicSensor sensor, int trigPin, int echoPin, Servo servo, int servoPin) throws Exception {
    this.arduino = arduino;
    this.sensor = sensor;
    this.servo = servo;

   
    arduino.connect(port);

    // TODO - FIX ME
    /*
    if (!sensor.attach(port, trigPin, echoPin)) {
      error("could not attach sensor");
      return false;
    }
    */

    // FIXME sensor.addRangeListener
    // publishRange --> onRange
    sensor.addRangeListener(this);
    servo.addServoEventListener(this);
    arduino.servoAttachPin(servo, servoPin);
    
    return true;
  }

  // UBER GOOD
  // UBER GOOD !!!! max & min complexity with service creation support with
  // Peers !!!!
  // attach (arduino port sensor trigPin echoPin servo servoPin ) <- max
  // complexity - no service creation
  // attach (port trigPin echoPin servoPin) <- min complexity - service
  // creation on peers
  public boolean attach(String port, int trigPin, int echoPin, int servoPin) throws Exception {
    return attach(arduino, port, sensor, trigPin, echoPin, servo, servoPin);
  }

  public Arduino getArduino() {
    return arduino;
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
  public Long onRange(Long range) {
    info("range %d", range);
    // filter too low
    // TODO this should be done on the Arduino
    if (range < 10) {
      return range;
    }

    rangeAvg += range;

    lastRange = range;
    ++rangeCount;

    /*
     * Point p = new Point(lastPos, range); invoke("publishPingdar", p);
     */
    return lastRange;
  }

  public Integer onServoEvent(Integer pos) {
    info("pos %d", pos);
    lastPos = pos;
    if (rangeCount > 0) {
      Point p = new Point(lastPos, rangeAvg / rangeCount);
      rangeAvg = 0;
      rangeCount = 0;
      invoke("publishPingdar", p);
    }

    return lastPos;
  }

  public Point publishPingdar(Point point) {
    return point;
  }

  @Override
  public void startService() {
    super.startService();
    arduino = (Arduino) startPeer("arduino");
    sensor = (UltrasonicSensor) startPeer("sensor");
    servo = (Servo) startPeer("servo");
  }

  public void stop() {
    super.stopService();
    sensor.stopRanging();
    servo.eventsEnabled(false);
    servo.stop();
  }

  public boolean sweep() {
    return sweep(sweepMin, sweepMax);
  }

  public boolean sweep(int sweepMin, int sweepMax) {
    this.sweepMin = sweepMin;
    this.sweepMax = sweepMax;
    this.step = 1; // FIXME STEP

    // TODO - configurable speed
    sensor = getSensor();
    servo = getServo();

    sensor.addRangeListener(this);
    servo.addServoEventListener(this);

    servo.setSpeed(0.20);
    servo.eventsEnabled(true);
    // STEP ???
    servo.sweep(sweepMin, sweepMax, 1, step);

    sensor.startRanging();
    return true;
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
    meta.addPeer("arduino", "Arduino", "arduino");
    meta.addPeer("sensor", "UltrasonicSensor", "sensor");
    meta.addPeer("servo", "Servo", "servo");

    return meta;
  }

}
