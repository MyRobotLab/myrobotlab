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

import static org.myrobotlab.service.OpenCV.BACKGROUND;
import static org.myrobotlab.service.OpenCV.FILTER_DETECTOR;
import static org.myrobotlab.service.OpenCV.FILTER_DILATE;
import static org.myrobotlab.service.OpenCV.FILTER_ERODE;
import static org.myrobotlab.service.OpenCV.FILTER_FACE_DETECT;
import static org.myrobotlab.service.OpenCV.FILTER_FIND_CONTOURS;
import static org.myrobotlab.service.OpenCV.FILTER_LK_OPTICAL_TRACK;
import static org.myrobotlab.service.OpenCV.FOREGROUND;
import static org.myrobotlab.service.OpenCV.PART;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterDetector;
import org.myrobotlab.opencv.OpenCVFilterGray;
import org.myrobotlab.opencv.OpenCVFilterPyramidDown;
import org.myrobotlab.opencv.OpenCVFilterTranspose;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.data.Rectangle;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
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

  transient public ArrayList<OpenCVFilter> preFilters = new ArrayList<OpenCVFilter>();

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
  transient public ServoController controller;
  transient public ServoControl x, y;

  // ------ PEER SERVICES END------
  // statistics
  public int updateModulus = 20;
  public long cnt = 0;
  public long latency = 0;

  // MRL points
  public Point2Df lastPoint = new Point2Df();

  private Double lastXServoPos;
  private Double lastYServoPos;

  // ----- INITIALIZATION DATA BEGIN -----
  public double xSetpoint = 0.5;
  public double ySetpoint = 0.5;

  // ----- INITIALIZATION DATA END -----

  int scanYStep = 2;
  int scanXStep = 2;

  public String LKOpticalTrackFilterName;

  double sizeIndexForBackgroundForegroundFlip = 0.10;

  /**
   * call back of all video data video calls this whenever a frame is processed
   * 
   * @param temp
   * @return
   */

  int faceFoundFrameCount = 0;

  int faceFoundFrameCountMin = 20;

  int faceLostFrameCount = 0;

  int faceLostFrameCountMin = 20;

  // -------------- System Specific Initialization End --------------

  boolean scan = false;

  // ------------------- tracking & detecting methods begin
  // ---------------------

  // FIXME !! question remains does the act of creating meta update the
  // reservatinos ?
  // e.g if I come to the party does the reservations get updated or do I
  // crash the party ??
  public Tracking(String n) throws Exception {
    super(n);
    // createPeer("X","Servo") <-- create peer of default type
    x = (ServoControl) createPeer("x");
    y = (ServoControl) createPeer("y");
    pid = (Pid) createPeer("pid");
    opencv = (OpenCV) createPeer("opencv");
    controller = (Arduino) createPeer("controller");

    // cache filter names
    LKOpticalTrackFilterName = String.format("%s.%s", opencv.getName(), FILTER_LK_OPTICAL_TRACK);
    opencv.addListener("publishOpenCVData", getName(), "setOpenCVData");

    setDefaultPreFilters();

    pid.setPID("x", 20.0, 5.0, 0.1);
    pid.setControllerDirection("x", Pid.DIRECTION_DIRECT);
    pid.setMode("x", Pid.MODE_AUTOMATIC);
    pid.setOutputRange("x", -20, 20); // <- not correct - based on maximum
    pid.setSampleTime("x", 30);
    pid.setSetpoint("x", 0.5); // set center

    pid.setPID("y", 20.0, 5.0, 0.1);
    pid.setControllerDirection("y", Pid.DIRECTION_DIRECT);
    pid.setMode("y", Pid.MODE_AUTOMATIC);
    pid.setOutputRange("y", -20, 20); // <- not correct - based on maximum
    pid.setSampleTime("y", 30);
    pid.setSetpoint("y", 0.5); // set center

    x.attach(controller);
    y.attach(controller);
    
  }

  public void addPreFilter(OpenCVFilter filter) {
    preFilters.add(filter);
  }

  public void clearPreFilters() {
    preFilters.clear();
  }

  // reset better ?
  public void clearTrackingPoints() {
    opencv.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "clearPoints");
    // reset position
    rest();
  }

  // -------------- System Specific Initialization Begin --------------

  public void faceDetect() {
    // opencv.addFilter("Gray"); needed ?
    opencv.removeFilters();

    log.info("starting faceDetect");

    for (int i = 0; i < preFilters.size(); ++i) {
      opencv.addFilter(preFilters.get(i));
    }

    // TODO single string static
    opencv.addFilter(FILTER_FACE_DETECT);
    opencv.setDisplayFilter(FILTER_FACE_DETECT);

    opencv.capture();
    opencv.publishOpenCVData(true);

    // wrong state
    setState(STATE_FACE_DETECT);

  }

  public void findFace() {
    scan = true;
  }

  public OpenCVData foundFace(OpenCVData data) {
    return data;
  }

  public ServoController getArduino() {
    return controller;
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
    return x;
  }

  public Pid getPID() {
    return pid;
  }

  public ServoControl getY() {
    return y;
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

  public void removeFilters() {
    opencv.removeFilters();
  }

  public void reset() {
    // TODO - reset pid values
    // clear filters
    opencv.removeFilters();
    // reset position
    rest();
  }

  public void rest() {
    log.info("rest");
    x.rest();
    y.rest();

    lastXServoPos = x.getPos();
    lastYServoPos = y.getPos();
  }

  public void scan() {

  }

  public void searchForeground() {

    ((OpenCVFilterDetector) opencv.getFilter(FILTER_DETECTOR)).search();

    setState(STATE_SEARCHING_FOREGROUND);
  }

  public void setDefaultPreFilters() {
    if (preFilters.size() == 0) {
      OpenCVFilterPyramidDown pd = new OpenCVFilterPyramidDown("PyramidDown");
      OpenCVFilterGray gray = new OpenCVFilterGray("Gray");
      preFilters.add(pd);
      preFilters.add(gray);
    }
  }
  
  public void addTransposeFilter() {
      OpenCVFilterTranspose transpose = new OpenCVFilterTranspose("Transpose");
      preFilters.add(transpose);
  }

  public void setForegroundBackgroundFilter() {
    opencv.removeFilters();
    for (int i = 0; i < preFilters.size(); ++i) {
      opencv.addFilter(preFilters.get(i));
    }
    opencv.addFilter(FILTER_DETECTOR);
    opencv.addFilter(FILTER_ERODE);
    opencv.addFilter(FILTER_DILATE);
    opencv.addFilter(FILTER_FIND_CONTOURS);

    ((OpenCVFilterDetector) opencv.getFilter(FILTER_DETECTOR)).learn();

    setState(STATE_LEARNING_BACKGROUND);
  }

  public void setIdle() {
    setState(STATE_IDLE);
  }

  public OpenCVData setOpenCVData(OpenCVData data) {

    switch (state) {

      case STATE_FACE_DETECT:
        // check for bounding boxes
        // data.setSelectedFilterName(FaceDetectFilterName);
        ArrayList<Rectangle> bb = data.getBoundingBoxArray();

        if (bb != null && bb.size() > 0) {

          // data.logKeySet();
          // log.error("{}",bb.size());

          // found face
          // find centroid of first bounding box
          lastPoint.x = bb.get(0).x + bb.get(0).width / 2;
          lastPoint.y = bb.get(0).y + bb.get(0).height / 2;
          updateTrackingPoint(lastPoint);

          ++faceFoundFrameCount;

          // dead zone and state shift
          if (faceFoundFrameCount > faceFoundFrameCountMin) {
            // TODO # of frames for verification
            invoke("foundFace", data);
            // data.saveToDirectory("data");
          }

        } else {
          // lost track

          faceFoundFrameCount = 0;

          if (scan) {
            double xpos = x.getPos();

            if (xpos + scanXStep >= Math.max(x.getMax(), x.getMin()) && scanXStep > 0 || xpos + scanXStep <= Math.min(x.getMin(), x.getMax()) && scanXStep < 0) {
              scanXStep = scanXStep * -1;
              int newY = (int)(Math.min(y.getMin(), y.getMax()) + (Math.random() * (Math.max(y.getMax(), y.getMin()) - Math.min(y.getMin(), y.getMax()))));
              y.moveTo(newY);
            }

            x.moveTo(xpos + scanXStep);
          }
          // state = STATE_FACE_DETECT_LOST_TRACK;
        }

        // if scanning stop scanning

        // if bounding boxes & no current tracking points
        // set set of tracking points in square - search for eyes?
        // find average point ?
        break;

        // FIXME - remove not used
        /*
      case STATE_FACE_DETECT_LOST_TRACK:
        int xpos = x.getPos();

        if (xpos >= Math.max(x.getMax(), x.getMin()) && scanXStep > 0) {
          scanXStep = scanXStep * -1;
        }

        if (xpos <= Math.min(x.getMin(), x.getMax()) && scanXStep < 0) {
          scanXStep = scanXStep * -1;
        }

        x.moveTo(xpos + scanXStep);

        break;
         */
      case STATE_IDLE:
        // setForegroundBackgroundFilter(); FIXME - setFGBGFilters for
        // different detection
        break;

      case STATE_LK_TRACKING_POINT:
        // extract tracking info
        // data.setSelectedFilterName(LKOpticalTrackFilterName);
        Point2Df targetPoint = data.getFirstPoint();
        if (targetPoint != null) {
          updateTrackingPoint(targetPoint);
        }
        break;

      case STATE_LEARNING_BACKGROUND:
        waitInterval = 3000;
        waitForObjects(data);
        break;

      case STATE_SEARCHING_FOREGROUND:
        waitInterval = 3000;
        waitForObjects(data);
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

  public void startLKTracking() {
    log.info("startLKTracking");

    opencv.removeFilters();

    for (int i = 0; i < preFilters.size(); ++i) {
      opencv.addFilter(preFilters.get(i));
    }

    opencv.addFilter(FILTER_LK_OPTICAL_TRACK, FILTER_LK_OPTICAL_TRACK);
    opencv.setDisplayFilter(FILTER_LK_OPTICAL_TRACK);

    opencv.capture();
    opencv.publishOpenCVData(true);

    setState(STATE_LK_TRACKING_POINT);
  }

  // DATA WHICH MUST BE SET BEFORE ATTACH METHODS !!!! - names must be set of
  // course !
  // com port
  // IMPORTANT CONCEPT - the Typed function should have ALL THE BUSINESS LOGIC
  // TO ATTACH
  // NON ANYWHERE ELSE !!
  @Override
  public void startService() {
    super.startService();
    x = (Servo) startPeer("x");
    y = (Servo) startPeer("y");
    pid = (Pid) startPeer("pid");
    controller = (Arduino) startPeer("controller");
    opencv = (OpenCV) startPeer("opencv");
    rest();
  }

  public void stopScan() {
    scan = false;
  }

  public void stopTracking() {
    opencv.removeFilters();
    setState(STATE_IDLE);
  }

  // --------------- publish methods begin ----------------------------
  public OpenCVData toProcess(OpenCVData data) {
    return data;
  }

  public void trackPoint() {
    trackPoint(0.5, 0.5);
  }

  public void trackPoint(Double x, Double y) {

    if (!STATE_LK_TRACKING_POINT.equals(state)) {
      startLKTracking();
    }

    opencv.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", x, y);
  }

  // GAAAAAAH figure out if (int , int) is SUPPORTED WOULD YA !
  public void trackPoint(int x, int y) {

    if (!STATE_LK_TRACKING_POINT.equals(state)) {
      startLKTracking();
    }
    opencv.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", x, y);
  }

  // FIXME - NEED A lost tracking event !!!!
  // FIXME - this is WAY TO OPENCV specific !
  // OpenCV should have a publishTrackingPoint method !
  // This should be updateTrackingPoint(Point2Df) & perhaps Point3Df :)
  final public void updateTrackingPoint(Point2Df targetPoint) {

    ++cnt;

    // describe this time delta
    latency = System.currentTimeMillis() - targetPoint.timestamp;
    log.info(String.format("pt %s", targetPoint));

    pid.setInput("x", targetPoint.x);
    pid.setInput("y", targetPoint.y);
    double currentXServoPos = x.getPos();
    double currentYServoPos = y.getPos();

    // TODO - work on removing currentX/YServoPos - and use the servo's
    // directly ???
    // if I'm at my min & and the target is further min - don't compute
    // pid
    if ((currentXServoPos <= Math.min(x.getMin(), x.getMax()) && xSetpoint - targetPoint.x < 0) || (currentXServoPos >= Math.max(x.getMin(), x.getMax()) && xSetpoint - targetPoint.x > 0)) {
      error(String.format("%f x limit out of range", currentXServoPos));
    } else {

      if (pid.compute("x")) {
        if(x.isInverted()) {
          currentXServoPos -= pid.getOutput("x");
        }
        else{
          currentXServoPos += pid.getOutput("x");
        }
        if (currentXServoPos != lastXServoPos) {
          // calamity- fix          
          x.moveTo(currentXServoPos);
          currentXServoPos = x.getPos();
          lastXServoPos = currentXServoPos;
        }
        // TODO - canidate for "move(int)" ?

      } else {
        log.warn("x data under-run");
      }
    }

    if ((currentYServoPos <= Math.min(y.getMin(), y.getMax()) && ySetpoint - targetPoint.y < 0) || (currentYServoPos >= Math.max(y.getMin(), y.getMax()) && ySetpoint - targetPoint.y > 0)) {
      error(String.format("%f y limit out of range", currentYServoPos));
    } else {
      if (pid.compute("y")) {
        if (y.isInverted()) {
          currentYServoPos -= (int) pid.getOutput("y");
        }
        else {
          currentYServoPos += (int) pid.getOutput("y");
        }
        if (currentYServoPos != lastYServoPos) {
          y.moveTo(currentYServoPos);
          currentYServoPos = y.getPos();
          lastYServoPos = currentYServoPos;
        }
      } else {
        log.warn("y data under-run");
      }
    }

    lastPoint = targetPoint;

    if (cnt % updateModulus == 0) {
      broadcastState(); // update graphics ?
      info(String.format("computeX %f computeY %f", pid.getOutput("x"), pid.getOutput("y")));
    }
  }

  public void waitForObjects(OpenCVData data) {
    data.setSelectedFilterName(FILTER_FIND_CONTOURS);
    ArrayList<Rectangle> objects = data.getBoundingBoxArray();
    int numberOfNewObjects = (objects == null) ? 0 : objects.size();

    // if I'm not currently learning the background and
    // countour == background ??
    // set state to learn background
    if (!STATE_LEARNING_BACKGROUND.equals(state) && numberOfNewObjects == 1) {
      SerializableImage img = new SerializableImage(data.getBufferedImage(), data.getSelectedFilterName());
      double width = img.getWidth();
      double height = img.getHeight();

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
          data.setAttribute(PART, BACKGROUND);
          invoke("toProcess", data);
          // ready to search foreground
          searchForeground();
        }
      } else {

        if (numberOfNewObjects > 0) {
          data.setAttribute(PART, FOREGROUND);
          invoke("toProcess", data);
        }
      }
    }

    lastNumberOfObjects = numberOfNewObjects;

  }

  public void connect(String port, int xPin, int yPin) throws Exception {
    connect(port, xPin, yPin, 0);
  }

  public void connect(String port, int xPin, int yPin, int cameraIndex) throws Exception {

    x.setPin(xPin);
    y.setPin(yPin);

    ((Microcontroller)controller).connect(port);

    controller.servoAttachPin(x, xPin);
    controller.servoAttachPin(y, yPin);
    opencv.setCameraIndex(cameraIndex);

    x.attach();
    y.attach();
    // TODO - think of a "validate" method
    x.moveTo(x.getRest() + 2);
    sleep(300);
    y.moveTo(y.getRest() + 2);
    sleep(300);
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
    meta.addDescription("proportional control, tracking, and translation");
    meta.addCategory("vision", "video", "sensor", "control");
    meta.addPeer("x", "Servo", "pan servo");
    meta.addPeer("y", "Servo", "tilt servo");
    meta.addPeer("pid", "Pid", "Pid service - for all your pid needs");
    meta.addPeer("opencv", "OpenCV", "Tracking OpenCV instance");
    meta.addPeer("controller", "Arduino", "Tracking Arduino instance");
    return meta;
  }

  public static void main(String[] args) {

    try {
      LoggingFactory.init(Level.INFO);
      int xPin = 13;
      int yPin = 12;
      String arduinoPort = "COM12";
      int cameraIndex = 0;

      // These all need to be created ahead of time o/w we get null pointer exceptions.
      // create the servos that will control our x and our y tracking.
      Servo x = (Servo)Runtime.createAndStart("tracker.x", "Servo");
      Servo y = (Servo)Runtime.createAndStart("tracker.y", "Servo");
      x.setPin(xPin);
      x.setVelocity(-1);
      y.setPin(yPin);
      y.setVelocity(-1);
      Arduino controller = (Arduino)Runtime.createAndStart("tracker.controller", "Arduino");
      controller.connect(arduinoPort);
      // now create the tracker service.
      Tracking tracker = (Tracking)Runtime.createAndStart("tracker", "Tracking");
      tracker.addTransposeFilter();
      
      //tracker.getY().setMinMax(79, 127);
      /*
       * tracker.getX().setPin(7); tracker.getY().setPin(8);
       * tracker.getOpenCV().setCameraIndex(1);
       */
      tracker.connect(arduinoPort, xPin, yPin, cameraIndex);
      // tracker.connect("COM4");
      tracker.startService();
      //tracker.faceDetect();
      tracker.startLKTracking();

      Runtime.start("gui", "SwingGui");
      // OpenCV opencv = tracker.getOpenCV();
      // opencv.setFrameGrabberType(OpenCV.GR)
      // .capture()
      // tracker.getGoodFeatures();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
