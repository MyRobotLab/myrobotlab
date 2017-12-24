/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Wii.IRData;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;

// TODO - BlockingQueue - + reference !

public class WiiDar extends Service {

  public final static class Point implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id = 0;
    public int type = 0;

    transient public IRData ir = null;

    public double z; // the computed z coordinate

    public int servoPos = 0;
    public int direction = -1;
    public long servoTime = 0;

    Point(int id, int servoPos, int direction, long servoTime) {
      this(id, servoPos, direction, servoTime, null);
    }

    Point(int id, int servoPos, int direction, long servoTime, IRData ir) {
      this(id, servoPos, direction, servoTime, ir, 0);
    }

    Point(int id, int servoPos, int direction, long servoTime, IRData ir, double z) {
      this.id = id;
      this.servoPos = servoPos;
      this.direction = direction;
      this.servoTime = servoTime;
      this.ir = ir;
      this.z = z;
    }

    Point(Point p) {
      this(p.id, p.servoPos, p.direction, p.servoTime, p.ir, p.z);
    }

    public int getX() {

      if (ir == null) {
        return -1;
      }

      return 1023 - ir.event.getAx(); // camera is mounted upside down in
      // the wii
    }

    public int getY() {
      if (ir == null) {
        return -1;
      }

      return ir.event.getAy(); // camera is mounted upside down in the wii
    }

    public double getZ() {
      return z;
    }

  }

  public class SweepMonolith implements Runnable {

    public boolean done = false;

    @Override
    public void run() {
      try {

        // BlockingQueue for synch pos, time, irevent, consumption etc.
        // http://www.javamex.com/tutorials/blockingqueue_example.shtml

        // use BlockingQueue

        // setting min max values
        servoLeftMax = 150;
        servoRightMax = 30;

        while (!done) {

          int servoPause = 30;
          // right scan
          for (int i = servoLeftMax; i > servoRightMax; --i) {
            irdata.clear();
            Point p = new Point(cnt, i, RIGHT, System.currentTimeMillis());
            servo.moveTo(i);
            Thread.sleep(servoPause); // wait for data "bleh" -
            // BlockingQueue?

            if (irdata.size() == 0) {
              // out of range - no new ir data
              p.z = -1;
            } else {
              p.ir = irdata.get(irdata.size() - 1); // get the
              // last one
              p.z = computeDepth(p.ir); // change to double?
            }

            // log.error(p.servoPos + " " + p.z);
            points.add(p);
            invoke("publishSinglePoint", new Point(p));
          }

          // invoke("publishArrayofPoints", copy(points));
          points.clear();

          // left scan
          for (int i = servoRightMax; i < servoLeftMax; ++i) {
            irdata.clear();
            Point p = new Point(cnt, i, LEFT, System.currentTimeMillis());
            servo.moveTo(i);
            Thread.sleep(servoPause);
            if (irdata.size() == 0) {
              // out of range - no new ir data
              p.z = -1;
            } else {
              p.ir = irdata.get(irdata.size() - 1); // get the
              // last one
              p.z = computeDepth(p.ir); // change to double?
            }
            points.add(p);
            invoke("publishSinglePoint", new Point(p));
          }

          // invoke("publishArrayofPoints", copy(points));
          points.clear();
        }

      } catch (InterruptedException e) {
        log.info("shutting down");
      }
    }
  }

  private static final long serialVersionUID = 1L;

  /*
   * // TODO remoe these service ----- DEBUG ONLY ------ BEGIN Wii wii = new
   * Wii("wii"); Arduino arduino = new Arduino("arduino"); Servo servo = new
   * Servo("servo"); // OpenCV opencv = new OpenCV("opencv"); SwingGui gui =
   * new SwingGui("gui"); // TODO remoe these service ----- DEBUG ONLY ------
   * END
   */

  public final static Logger log = LoggerFactory.getLogger(WiiDar.class);

  // TODO - possibly initialize - must contend with gui as well as arduino wii
  // & servo
  // public Wii wii = null;
  transient public Servo servo = null;
  public static final int LEFT = 0;

  public static final int RIGHT = 1;
  public static final int UNKNOWN = -1;

  transient public IRData lastIRData = null;
  transient public Pin lastEncoderData = null;
  int servoRightMax = 0;
  transient IRData irRightMax = null;
  int servoLeftMax = 0;

  transient IRData irLeftMax = null;
  boolean calibrating = true;
  transient ArrayList<Point> points = new ArrayList<Point>();
  transient ArrayList<Point> leftCalibrated = new ArrayList<Point>();
  transient ArrayList<Point> rightCalibrated = new ArrayList<Point>();

  transient ArrayList<ArrayList<Point>> left = new ArrayList<ArrayList<Point>>();

  transient ArrayList<ArrayList<Point>> right = new ArrayList<ArrayList<Point>>();
  transient ArrayList<IRData> irdata = new ArrayList<IRData>();

  /*
   * WiiDar point represents a piece of data for ranging. It has a synced (by
   * multiple methods) IRData and Servo Data. The synchronization can be
   * software technique ie. pausing the system until a the servo command to move
   * to a location has propagated to an IREvent. Or, preferably it would be a
   * hardware event where an motor's encoder data is paired with the appropriate
   * IREvent.
   */

  transient Thread sweeperThread = null;

  transient SweepMonolith sweep = null;

  int cnt = 0;

  // BockingQueue <Point> points
  int width = 1024;

  int height = 768;

  double pixelsPerDegree = 24;

  // publishing points end ---------------
  /*
   * public void calibrateLaserServoSweep() { try { int cnt = 0;
   * 
   * // in calibration mode invoke("setCalibrating", true);
   * 
   * // move servo all the way to the right // increment servo left until IRData
   * appears servo.moveTo(60); Thread.sleep(1000); lastIRData = null;
   * lastEncoderData = null; arduino.pinMode(4, Arduino.INPUT); // set the
   * encoder to input mode TODO - this could be bug worthy
   * 
   * for (int i = 60; i < 180; ++i) { servo.moveTo(i); Thread.sleep(30); if
   * (lastIRData != null) { invoke("setServoRightMax", i);
   * invoke("setIRRightMax", lastIRData); break; } }
   * 
   * // move servo all the way to the left // scan right until IRData appears
   * servo.moveTo(180); Thread.sleep(1000); lastIRData = null; lastEncoderData =
   * null;
   * 
   * for (int i = 180; i > 0; --i) { servo.moveTo(i); Thread.sleep(30); if
   * (lastIRData != null) { invoke("setServoLeftMax", i); invoke("setIRLeftMax",
   * lastIRData); break; } }
   * 
   * 
   * // right scan for (int i = servoLeftMax; i > servoRightMax; --i) { Point p
   * = new Point(i, i, RIGHT, System.currentTimeMillis()); servo.moveTo(i);
   * Thread.sleep(30); p.ir = lastIRData; rightCalibrated.add(p); }
   * 
   * invoke("setRightCalibrated", rightCalibrated);
   * 
   * // left scan for (int i = servoRightMax; i < servoLeftMax; ++i) { Point p =
   * new Point(i, i, LEFT, System.currentTimeMillis()); servo.moveTo(i);
   * Thread.sleep(30); p.ir = lastIRData; leftCalibrated.add(p); }
   * 
   * invoke("setLeftCalibrated", leftCalibrated);
   * 
   * // out of calibration mode invoke("setCalibrating", false); while (cnt <
   * 15000) {
   * 
   * int servoPause = 20; // right scan for (int i = servoLeftMax; i >
   * servoRightMax; --i) { Point p = new Point(cnt, i, RIGHT,
   * System.currentTimeMillis()); servo.moveTo(i); Thread.sleep(servoPause);
   * p.ir = lastIRData; points.add(p); }
   * 
   * invoke("publishSweepData", copy(points)); points.clear();
   * 
   * // left scan for (int i = servoRightMax; i < servoLeftMax; ++i) { Point p =
   * new Point(cnt, i, LEFT, System.currentTimeMillis()); servo.moveTo(i);
   * Thread.sleep(servoPause); p.ir = lastIRData; points.add(p); }
   * 
   * invoke("publishSweepData", copy(points)); points.clear();
   * 
   * ++cnt; } } catch (InterruptedException e) { // TODO Auto-generated catch
   * block logException(e); }
   * 
   * }
   */
  static final public ArrayList<Point> copy(ArrayList<Point> points) {
    if (points == null) {
      return null;
    }

    ArrayList<Point> p = new ArrayList<Point>(points.size());
    for (int i = 0; i < points.size(); ++i) {
      p.add(new Point(points.get(i)));
    }

    return p;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);
    try {

      WiiDar wiidar = new WiiDar("wiidar");
      wiidar.startService();
      Runtime.createAndStart("gui", "SwingGui");
      // wiidar.startRobot();

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public WiiDar(String n) {
    super(n);
  }

  public double computeDepth(IRData ir) {
    // int pixels = 1023 - 473;
    int pixels = 1023 - ir.event.getAx();

    // range point
    double A; // laser static angle
    double B; // camera angle
    double C; // point's angle

    double a; // this is what I want, it "should be" distance from the wii
    // camera
    // double b; // distance from the laser - don't care
    double c = 4; // distance from camera to laser

    A = Math.toRadians(61.5);

    if (pixels > width / 2) { // obtuse triangle
      B = Math.toRadians(90 + ((pixels - width / 2) / pixelsPerDegree));
    } else {
      // acute triangle
      B = Math.toRadians(90 - ((width / 2 - pixels) / pixelsPerDegree));
    }
    C = Math.toRadians(180) - (B + A);

    // Law of Sines, like stop sines
    a = (c * Math.sin(A)) / Math.sin(C);

    return a;
  }

  public ArrayList<Point> publishArrayofPoints(ArrayList<Point> points) {
    return points;
  }

  public IRData publishIR(IRData ird) {
    irdata.add(ird);
    lastIRData = ird;
    return ird;
  }

  /*
   * public void initialize (Wii wii, Servo servo) { this.wii = wii; this.servo
   * = servo;
   * 
   * // setting up wii wii.getWiimotes(); wii.setSensorBarAboveScreen();
   * wii.activateIRTRacking(); wii.setIrSensitivity(5); // 1-5 (highest)
   * wii.activateListening();
   * 
   * // send data from the wii to wiidar wii.addListener(this, "publishIR",
   * IRData.class.getCanonicalName());
   * 
   * }
   * 
   * 
   * public void startRobot() { // setting up servo
   * servo.attach(arduino.getName(), 9);
   * 
   * // setting up wii wii.getWiimotes(); wii.setSensorBarAboveScreen();
   * wii.activateIRTRacking(); wii.setIrSensitivity(5); // 1-5 (highest)
   * wii.activateListening();
   * 
   * // starting services gui.start(); servo.start(); arduino.start();
   * //opencv.start(); wii.start();
   * 
   * // TODO - make note that pinMode - will get lost if not done after serial
   * communication is establised // setting listeners/notifiers
   * 
   * // send data from the wii to wiidar wii.addListener(this, "publishIR",
   * IRData.class.getCanonicalName()); // data from widar to the gui
   * addListener("publishArrayofPoints", gui.getName(),"displaySweepData",
   * Point.class.getCanonicalName());
   * 
   * // send the data from the wii to wiidar // wii.addListener("publishIR",
   * this.getName(), "computeDepth", IRData.class.getCanonicalName()); // send
   * the computed depth &amp; data to the gui // addListener("computeDepth",
   * gui.getName(),"publishSinglePoint", Point.class.getCanonicalName());
   * addListener("publishSinglePoint", gui.getName(),"publishSinglePoint",
   * Point.class.getCanonicalName()); // gui.addListener("processImage",
   * opencv.getName(),"input", BufferedImage.class.getCanonicalName());
   * //wii.addListener("publishPin", this.getName(), "publishPin",
   * IRData.class.getCanonicalName()); arduino.addListener(this,
   * SensorData.publishPin, PinData.class.getCanonicalName());
   * //wii.addListener(
   * 
   * 
   * 
   * 
   * sweepMonolith(); //calibrateLaserServoSweep();
   * 
   * }
   */

  public Pin onPin(Pin pd) {
    ++cnt;
    lastEncoderData = pd;
    // log.error("pin v " + pd.value + " " + cnt + " " + pd.time);
    return pd;
  }

  public Point publishSinglePoint(Point point) {
    return point;
  }

  public ArrayList<Point> publishSweepData(ArrayList<Point> d) {
    return d;
  }

  public Boolean setCalibrating(Boolean t) {
    calibrating = t;
    return t;
  }

  public IRData setIRLeftMax(IRData max) {
    irLeftMax = max;
    return max;
  }

  public IRData setIRRightMax(IRData max) {
    irRightMax = max;
    return max;
  }

  public ArrayList<Point> setLeftCalibrated(ArrayList<Point> d) {
    leftCalibrated = d;
    return leftCalibrated;
  }

  public ArrayList<Point> setRightCalibrated(ArrayList<Point> d) {
    rightCalibrated = d;
    return rightCalibrated;
  }

  // publishing points begin ---------------
  public Integer setServoLeftMax(Integer max) {
    servoLeftMax = max;
    return max;
  }

  public Integer setServoRightMax(Integer max) {
    servoRightMax = max;
    return max;
  }

  public void startSweep() {
    if (sweep == null) {
      sweep = new SweepMonolith();
    }
    sweep.done = false;
    sweeperThread = new Thread(sweep);
    sweeperThread.start();
  }

  @Override
  public void stopService() {
    if (sweep != null)
      sweep.done = true;

    sweeperThread = null;

    super.stopService();
  }

  public void stopSweep() {
    if (sweep != null)
      sweep.done = true;

    sweeperThread = null;
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
    ServiceType meta = new ServiceType(WiiDar.class.getCanonicalName());
    meta.addDescription("ranging using a wiimote");
    meta.addDependency("wiiuse.wiimote", "0.12b");
    meta.addCategory("sensor");
    // no longer have hardware for this ...
    meta.setAvailable(false);
    return meta;
  }

}
