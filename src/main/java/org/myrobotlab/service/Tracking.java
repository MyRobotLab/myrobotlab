/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import static org.myrobotlab.service.OpenCV.BACKGROUND;
import static org.myrobotlab.service.OpenCV.FILTER_DETECTOR;
import static org.myrobotlab.service.OpenCV.FILTER_FACE_DETECT;
import static org.myrobotlab.service.OpenCV.FILTER_FIND_CONTOURS;
import static org.myrobotlab.service.OpenCV.FILTER_LK_OPTICAL_TRACK;
import static org.myrobotlab.service.OpenCV.FOREGROUND;
import static org.myrobotlab.service.OpenCV.PART;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilterDetector;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

// TODO - attach() ???  Static name peer key list ???

/**
 * 
 * Tracking - This service connects to the video stream from OpenCV It then uses
 * LK tracking for a point in the video stream. As that point moves the x and y
 * servos that are attached to a camera will move to keep the point in the
 * screen. (controlling yaw and pitch.)
 *
 */
public class Tracking extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Tracking.class.getCanonicalName());

  long lastTimestamp = 0;
  long waitInterval = 5000;
  int lastNumberOfObjects = 0;

  // Tracking states - TODO split states into groups
  public final static String STATE_LK_TRACKING_POINT = "state lucas kanade tracking";
  public final static String STATE_IDLE = "state idle";
  public final static String STATE_NEED_TO_INITIALIZE = "state initializing";
  public static final String STATUS_CALIBRATING = "state calibrating";
  public static final String STATE_LEARNING_BACKGROUND = "state learning background";
  public static final String STATE_SEARCH_FOREGROUND = "state search foreground";
  public static final String STATE_SEARCHING_FOREGROUND = "state searching foreground";
  public static final String STATE_WAITING_FOR_OBJECTS_TO_STABILIZE = "state waiting for objects to stabilize";
  public static final String STATE_WAITING_FOR_OBJECTS_TO_DISAPPEAR = "state waiting for objects to disappear";
  public static final String STATE_STABILIZED = "state stabilized";
  public static final String STATE_FACE_DETECT = "state face detect";

  // memory constants
  private String state = STATE_IDLE;

  // ------ PEER SERVICES BEGIN------
  transient public Pid pid;
  transient public OpenCV opencv;

  // transient public ServoControl x, y;
  private class TrackingServoData {
    transient ServoControl servoControl = null;
    int scanStep = 2;
    Double currentServoPos;
    String name;
    String axis;

    TrackingServoData(String name) {
      this.name = name;
    }
  }

  transient private HashMap<String, TrackingServoData> servoControls = new HashMap<String, TrackingServoData>();

  // statistics
  public int updateModulus = 1;
  
  public long cnt = 0;
  
  public Point2df lastPoint = new Point2df(0.5F, 0.5F);

  double sizeIndexForBackgroundForegroundFlip = 0.10;

  int faceFoundFrameCount = 0;
  
  int faceFoundFrameCountMin = 2;
  
  boolean scan = false;

  String[] axis = new String[] { "x", "y" };

  // FIXME !! question remains does the act of creating meta update the
  // reservatinos ?
  // e.g if I come to the party does the reservations get updated or do I
  // crash the party ??
  public Tracking(String n, String id) throws Exception {
    super(n, id);

    pid = (Pid) createPeer("pid");
    // the kp should be proportional to the input min/max of the servo.. for now
    // we'll go with 45 for now.
    pid.setPID("x", 3.0, 1.0, 0.1);
    pid.setControllerDirection("x", Pid.DIRECTION_DIRECT);
    pid.setMode("x", Pid.MODE_AUTOMATIC);
    pid.setOutputRange("x", -5, 5); // <- not correct - based on maximum
    pid.setSampleTime("x", 30);
    pid.setSetpoint("x", 0.5); // set center

    pid.setPID("y", 3.0, 1.0, 0.1);
    pid.setControllerDirection("y", Pid.DIRECTION_DIRECT);
    pid.setMode("y", Pid.MODE_AUTOMATIC);
    pid.setOutputRange("y", 5, -5); // <- not correct - based on maximum
    pid.setSampleTime("y", 30);
    pid.setSetpoint("y", 0.5); // set center

  }

  // reset better ?
  public void clearTrackingPoints() {
    opencv.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "clearPoints");
    // reset position
    rest();
  }

  /**
   * generic method to compute filter output after setState()
   */
  public void execFilterFunctions(String filterName, String state) {
    if (!Arrays.asList(OpenCV.getPossibleFilters()).contains(filterName)) {
      log.error("Sorry, {} is an unknown filter.", filterName);
      return;
    }
    log.info("starting {} related to {}", state, filterName);
    clearTrackingPoints();
    if (opencv.getFilter(filterName) == null) {
      opencv.addFilter(filterName);
    }
    opencv.setActiveFilter(filterName);
    opencv.capture();
    setState(state);
  }

  public void faceDetect() {
    execFilterFunctions(FILTER_FACE_DETECT, STATE_FACE_DETECT);
  }

  public void startLKTracking() {
    execFilterFunctions(FILTER_LK_OPTICAL_TRACK, STATE_LK_TRACKING_POINT);
  }

  public void trackPoint() {
    trackPoint(0.5F, 0.5F);
  }

  public void trackPoint(Float x, Float y) {

    if (!STATE_LK_TRACKING_POINT.equals(state)) {
      startLKTracking();
    }
    opencv.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", x, y);
  }

  public void findFace() {
    scan = true;
  }

  public OpenCVData foundFace(OpenCVData data) {
    return data;
  }

  // TODO - enhance with location - not just heading
  // TODO - array of attributes expanded Object[] ... ???
  // TODO - use GEOTAG - LAT LONG ALT DIRECTION LOCATION CITY GPS TIME OFFSET
  /*
   * public OpenCVData setLocation(OpenCVData data) {
   * data.setX(x.getPosition()); data.setY(y.getPosition()); return data; }
   */

  // ------------------- tracking & detecting methods end
  // ---------------------

  public OpenCV getOpenCV() {
    return opencv;
  }

  public String getState() {
    return state;
  }

  public ServoControl getX() {
    return servoControls.get("x").servoControl;
  }

  public Pid getPID() {
    return pid;
  }

  public ServoControl getY() {
    return servoControls.get("y").servoControl;
  }

  // --------------- publish methods end ----------------------------

  public boolean isIdle() {
    return STATE_IDLE.equals(state);
  }

  public void learnBackground() {
    ((OpenCVFilterDetector) opencv.getFilter(FILTER_DETECTOR)).learn();
    setState(STATE_LEARNING_BACKGROUND);
  }

  // ubermap !!!
  // for (Object key : map.keySet())
  // map.get(key))
  public void publish(HashMap<String, SerializableImage> images) {
    for (Map.Entry<String, SerializableImage> o : images.entrySet()) {
      // Map.Entry<String,SerializableImage> pairs = o;
      log.info(o.getKey());
      publish(o.getValue());
    }
  }

  public void publish(SerializableImage image) {
    invoke("publishFrame", image);
  }

  public SerializableImage publishFrame(SerializableImage image) {
    return image;
  }

  public void rest() {
    log.info("rest");
    for (TrackingServoData sc : servoControls.values()) {
   
        Double velocity = sc.servoControl.getSpeed();
        sc.servoControl.setSpeed(20.0);
        sc.servoControl.moveToBlocking(sc.servoControl.getRest());
        sc.servoControl.setSpeed(velocity);
      
    }
  }

  public void searchForeground() {
    ((OpenCVFilterDetector) opencv.getFilter(FILTER_DETECTOR)).search();
    setState(STATE_SEARCHING_FOREGROUND);
  }

  public void setIdle() {
    setState(STATE_IDLE);
  }

  public OpenCVData onOpenCVData(OpenCVData data) {
    SerializableImage img = new SerializableImage(data.getDisplay(), data.getSelectedFilter());
    float width = img.getWidth();
    float height = img.getHeight();
    switch (state) {

      case STATE_FACE_DETECT:
        // check for bounding boxes
        List<Rectangle> bb = data.getBoundingBoxArray();

        if (bb != null && bb.size() > 0) {

          // found face
          // find centroid of first bounding box
          Point2df thisPoint = new Point2df();
          thisPoint.x = ((bb.get(0).x + bb.get(0).width / 2) / width);
          thisPoint.y = ((bb.get(0).y + bb.get(0).height / 2) / height);

          // keep calm and save MORE cpu!
          if (thisPoint != lastPoint) {
            updateTrackingPoint(thisPoint);
          }

          ++faceFoundFrameCount;

          // dead zone and state shift
          if (faceFoundFrameCount > faceFoundFrameCountMin) {
            // TODO # of frames for verification
            log.info("found face");
            invoke("foundFace", data);
            // ensure bumpless transfer ??
            // pid.init("x");
            // pid.init("y");
            // data.saveToDirectory("data");
          }

        } else {
          // lost track
          // log.info("Lost track...");
          faceFoundFrameCount = 0;
          if (scan) {
            log.info("Scan enabled...");
            TrackingServoData x = servoControls.get("x");
            TrackingServoData y = servoControls.get("y");
            double xpos = x.servoControl.getPos();
            if (xpos + x.scanStep >= x.servoControl.getMax() && x.scanStep > 0 || xpos + x.scanStep <= x.servoControl.getMin() && x.scanStep < 0) {
              x.scanStep *= -1;
              double newY = y.servoControl.getMin() + (Math.random() * (y.servoControl.getMax() - y.servoControl.getMin()));
              y.servoControl.moveTo(newY);
            }
            x.servoControl.moveTo(xpos + x.scanStep);
          }
          // state = STATE_FACE_DETECT_LOST_TRACK;
        }

        // if scanning stop scanning

        // if bounding boxes & no current tracking points
        // set set of tracking points in square - search for eyes?
        // find average point ?
        break;

      case STATE_IDLE:
        lastPoint.x = 0.5F;
        lastPoint.y = 0.5F;
        // setForegroundBackgroundFilter(); FIXME - setFGBGFilters for
        // different detection
        break;

      // TODO: test startLKTracking -> maybe fix targetPoint.get(0) for image
      // proportion between 0>1
      case STATE_LK_TRACKING_POINT:
        // extract tracking info
        // data.setSelectedFilterName(LKOpticalTrackFilterName);
        List<Point2df> targetPoint = data.getPointArray();
        if (targetPoint != null && targetPoint.size() > 0) {
          targetPoint.get(0).x = targetPoint.get(0).x / width;
          targetPoint.get(0).y = targetPoint.get(0).y / height;
          // keep calm and save MORE cpu!
          if (targetPoint.get(0) != lastPoint) {
            updateTrackingPoint(targetPoint.get(0));
          }
        }
        break;

      case STATE_LEARNING_BACKGROUND:
        waitInterval = 3000;
        waitForObjects(data, width, height);
        break;

      case STATE_SEARCHING_FOREGROUND:
        waitInterval = 3000;
        waitForObjects(data, width, height);
        break;

      default:
        error("recieved opencv data but unknown state");
        break;
    }

    return data;
  }

  public void setState(String newState) {
    state = newState;
    info(state);
  }

  public void stopScan() {
    scan = false;
  }

  public void stopTracking() {
    log.info("stop tracking, all filters disabled");
    setState(STATE_IDLE);
    clearTrackingPoints();
    opencv.disableAll();
  }

  // --------------- publish methods begin ----------------------------
  public OpenCVData toProcess(OpenCVData data) {
    return data;
  }

  // FIXME - NEED A lost tracking event !!!!
  // FIXME - this is WAY TO OPENCV specific !
  // OpenCV should have a publishTrackingPoint method !
  // This should be updateTrackingPoint(Point2Df) & perhaps Point3Df :)
  final public void updateTrackingPoint(Point2df targetPoint) {

    ++cnt;

    // describe this time delta
    // latency = System.currentTimeMillis() - targetPoint.timestamp;
    log.info("Update Tracking Point {}", targetPoint);

    // pid.setInput("x", targetPoint.x);
    // pid.setInput("y", targetPoint.y);

    // TODO - work on removing currentX/YServoPos - and use the servo's
    // directly ???
    // if I'm at my min & and the target is further min - don't compute
    // pid
    for (TrackingServoData tsd : servoControls.values()) {
      pid.setInput(tsd.axis, targetPoint.get(tsd.axis));
      if (pid.compute(tsd.name)) {
        // TODO: verify this.. we want the pid output to be the input for our
        // servo..min/max are input min/max on the servo to
        // ensure proper scaling
        // of values between services.
        tsd.currentServoPos += pid.getOutput(tsd.name);
        tsd.servoControl.moveTo(tsd.currentServoPos);
        tsd.currentServoPos = tsd.servoControl.getPos();
      } else {
        log.warn("{} data under-run", tsd.servoControl.getName());
      }
    }

    lastPoint = targetPoint;

    if (cnt % updateModulus == 0) {
      // moz4r : //keep calm and save MORE cpu!
      // broadcastState(); // update graphics ?
      // info(String.format("computeX %f computeY %f", pid.getOutput("x"),
      // pid.getOutput("y")));
    }
  }

  public void waitForObjects(OpenCVData data, float width, float height) {
    data.setSelectedFilter(FILTER_FIND_CONTOURS);
    List<Rectangle> objects = data.getBoundingBoxArray();
    int numberOfNewObjects = (objects == null) ? 0 : objects.size();

    // if I'm not currently learning the background and
    // countour == background ??
    // set state to learn background
    if (!STATE_LEARNING_BACKGROUND.equals(state) && numberOfNewObjects == 1) {
      Rectangle rect = objects.get(0);
      // publish(data.getImages());
      if ((width - rect.width) / width < sizeIndexForBackgroundForegroundFlip && (height - rect.height) / height < sizeIndexForBackgroundForegroundFlip) {
        learnBackground();
        info(String.format("%s - object found was nearly whole view - foreground background flip", state));
      }
    }

    if (numberOfNewObjects != lastNumberOfObjects) {
      info(String.format("%s - unstable change from %d to %d objects - reset clock - was stable for %d ms limit is %d ms", state, lastNumberOfObjects, numberOfNewObjects,
          System.currentTimeMillis() - lastTimestamp, waitInterval));
      lastTimestamp = System.currentTimeMillis();
    }

    if (waitInterval < System.currentTimeMillis() - lastTimestamp) {
      // setLocation(data);
      // number of objects have stated the same
      if (STATE_LEARNING_BACKGROUND.equals(state)) {
        if (numberOfNewObjects == 0) {
          // process background
          // data.putAttribute(BACKGROUND);
          data.put(PART, BACKGROUND);
          invoke("toProcess", data);
          // ready to search foreground
          searchForeground();
        }
      } else {

        if (numberOfNewObjects > 0) {
          data.put(PART, FOREGROUND);
          invoke("toProcess", data);
        }
      }
    }

    lastNumberOfObjects = numberOfNewObjects;

  }

  public void connect(OpenCV opencv, ServoControl x, ServoControl y) {
    log.info("Connect 2 servos for head tracking!... aye aye captain.  Also.. an open cv instance.");
    attach(opencv);
    attach(x, y);
    // don't understand this, it should be getMin and getMax, no ? but
    // seem worky..
    pid.setOutputRange("x", -x.getMax(), x.getMax());
    pid.setOutputRange("y", -y.getMax(), y.getMax());
    // target the center !
    pid.setSetpoint("x", 0.5);
    pid.setSetpoint("y", 0.5);
    // TODO: remove this sleep stmt
    sleep(100);
    rest();
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

    ServiceType meta = new ServiceType(Tracking.class.getCanonicalName());
    meta.addDescription("uses a video input and vision library to visually track objects");
    meta.addCategory("vision", "video", "sensors", "control");
    meta.addPeer("pid", "Pid", "Pid service - for all your pid needs");
    meta.addPeer("opencv", "OpenCV", "Tracking OpenCV instance");
    return meta;
  }

  public void attach(ServoControl servo, String axis) {
    if (!(axis.equals("x") || axis.equals("y"))) {
      log.info("Axis must be x or y");
      return;
    }
    TrackingServoData tsd = new TrackingServoData(axis);
    tsd.servoControl = servo;
    tsd.axis = axis;
    servoControls.put(axis, tsd);
    tsd.currentServoPos = servo.getPos();
    log.info("Tracking attach : Axis : {} Servo {} current position {}", axis, servo.getName(), servo.getPos());
  }

  public void attach(ServoControl x, ServoControl y) {
    attach(x, "x");
    attach(y, "y");
  }

  public void attach(OpenCV opencv) {
    this.opencv = opencv;
    opencv.addListener("publishOpenCVData", getName(), "onOpenCVData");
  }

  /**
   * Routing attach - routes ServiceInterface.attach(service) to appropriate
   * methods for this class
   */
  @Override
  public void attach(Attachable service) throws Exception {
    if (OpenCV.class.isAssignableFrom(service.getClass())) {
      attach((OpenCV) service);
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  public static void main(String[] args) {

    try {
      LoggingFactory.init(Level.INFO);

      Runtime.start("gui", "SwingGui");
      // Runtime.start("webgui", "WebGui");

      VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
      virtual.connect("COM3");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("COM3");

      Tracking t01 = (Tracking) Runtime.start("t01", "Tracking");
      Servo rothead = (Servo) Runtime.start("rothead", "Servo");
      Servo neck = (Servo) Runtime.start("neck", "Servo");
      rothead.attach(arduino, 0);
      neck.attach(arduino, 1);
      OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
      t01.connect(opencv, rothead, neck);
      opencv.capture();
      // t01.trackPoint();
      t01.faceDetect();
      // tracker.getGoodFeatures();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}