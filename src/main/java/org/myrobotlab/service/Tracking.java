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
import static org.myrobotlab.service.OpenCV.FILTER_FACE_RECOGNIZER; 
import static org.myrobotlab.service.OpenCV.FILTER_GRAY; 
import static org.myrobotlab.service.OpenCV.FILTER_PYRAMID_DOWN;
import static org.myrobotlab.service.OpenCV.FILTER_LK_OPTICAL_TRACK;
import static org.myrobotlab.service.OpenCV.FOREGROUND;
import static org.myrobotlab.service.OpenCV.PART;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterDetector;
import org.myrobotlab.opencv.OpenCVFilterFaceRecognizer;
import org.myrobotlab.opencv.OpenCVFilterGray;
import org.myrobotlab.opencv.OpenCVFilterPyramidDown;
import org.myrobotlab.opencv.OpenCVFilterTranspose;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.data.Rectangle;
import org.myrobotlab.service.interfaces.PortConnector;
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

  // ------ PEER SERVICES END------
  // statistics
  public int updateModulus = 1;
  public long cnt = 0;
  public long latency = 0;

  // MRL points
  public Point2Df lastPoint = new Point2Df();

  public String LKOpticalTrackFilterName;

  double sizeIndexForBackgroundForegroundFlip = 0.10;

  /**
   * call back of all video data video calls this whenever a frame is processed
   * 
   */
  //TODO: should be a function of the current frame rate  for now, require at least 1.
  int faceFoundFrameCount = 0;
  int faceFoundFrameCountMin = 2;
  //int faceLostFrameCount = 0;
  //int faceLostFrameCountMin = 20;
  // -------------- System Specific Initialization End --------------

  boolean scan = false;

  // ------------------- tracking & detecting methods begin
  // ---------------------

  String[] axis = new String[] { "x", "y" };

  // FIXME !! question remains does the act of creating meta update the
  // reservatinos ?
  // e.g if I come to the party does the reservations get updated or do I
  // crash the party ??
  public Tracking(String n) throws Exception {
    super(n);

    pid = (Pid) createPeer("pid");
    setDefaultPreFilters();
    // the kp should be propotional to the input min/max of the servo.. for now we'll go with 45 for now.
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

  public OpenCVFilter faceDetect(boolean PleaseRecognizeToo) {
    // opencv.addFilter("Gray"); needed ?    
    stopTracking();
    log.info("starting faceDetect");
    for (int i = 0; i < preFilters.size(); ++i) {
      //grayFilter+Facerecognition=crash
      if (preFilters.get(i).name==FILTER_GRAY && PleaseRecognizeToo)
      {
        log.info("skip gray filter for faceRecognize");
      }
      else
      {
      opencv.addFilter(preFilters.get(i));
      }
    }
    OpenCVFilter fr=null;
    if (PleaseRecognizeToo)
    {
      fr=opencv.addFilter(FILTER_FACE_RECOGNIZER);
    }
    opencv.addFilter(FILTER_FACE_DETECT);
    opencv.setDisplayFilter(FILTER_FACE_DETECT);
    opencv.capture();
    opencv.publishOpenCVData(true);
    setState(STATE_FACE_DETECT);
    return fr;
  }
  
  public void faceDetect() {
    faceDetect(false);
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

  public void removeFilters() {
    opencv.removeFilters();
    sleep(1000);
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
    if (controller == null) {
      return;
    }
    String controllerName = controller.getName();
    for (TrackingServoData sc : servoControls.values()) {
      if (sc.servoControl.isAttached(controllerName)) {
        sc.servoControl.rest();
      }
    }
  }

  public void scan() {

  }

  public void searchForeground() {
    ((OpenCVFilterDetector) opencv.getFilter(FILTER_DETECTOR)).search();
    setState(STATE_SEARCHING_FOREGROUND);
  }

  public void setDefaultPreFilters() {
    if (preFilters.size() == 0) {
      OpenCVFilterPyramidDown pd = new OpenCVFilterPyramidDown(FILTER_PYRAMID_DOWN);
      OpenCVFilterGray gray = new OpenCVFilterGray(FILTER_GRAY);
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

  public OpenCVData onOpenCVData(OpenCVData data) {
    // log.info("OnOpenCVData");
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
            if (xpos + x.scanStep >= x.servoControl.getMaxInput() && x.scanStep > 0 || xpos + x.scanStep <= x.servoControl.getMinInput() && x.scanStep < 0) {
              x.scanStep *= -1;
              double newY = y.servoControl.getMinInput() + (Math.random() * (y.servoControl.getMaxInput() - y.servoControl.getMinInput()));
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

    TrackingServoData x = new TrackingServoData("x");
    x.servoControl = (ServoControl) startPeer("x");
    servoControls.put("x", x);

    TrackingServoData y = new TrackingServoData("y");
    y.servoControl = (ServoControl) startPeer("y");
    servoControls.put("y", y);

    controller = (Arduino) startPeer("controller");
    pid = (Pid) startPeer("pid");
    opencv = (OpenCV) startPeer("opencv");
    rest();
  }

  public void stopScan() {
    scan = false;
  }

  public void stopTracking() {
    log.info("stop tracking");
    setState(STATE_IDLE);
    removeFilters();
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
        // TODO: verify this.. we want the pid output to be the input for our servo..min/max are input min/max on the servo to ensure proper scaling 
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

  public void connect(OpenCV opencv, Servo x, Servo y) {
    log.info("Connect 2 servos for head tracking!... aye aye captain.  Also.. an open cv instance.");
    attach(opencv);
    attach(x, y);    
    // TODO: consider using the input min/max of the servo here..
    pid.setOutputRange("x", -20, 20);
    pid.setOutputRange("y", -20, 20);
    // target the center !
    pid.setSetpoint("x", 0.5);
    pid.setSetpoint("y", 0.5);
    // TODO: remove this sleep stmt
    sleep(100);
    rest();
  }
  
  
  public void connect(String port, int xPin, int yPin) throws Exception {
    connect(port, xPin, yPin, 0);
  }

  public void connect(String port, int xPin, int yPin, int cameraIndex) throws Exception {
    int[] pins = new int[] { xPin, yPin };
    controller = (Arduino) startPeer("controller");
    ((PortConnector) controller).connect(port);

    for (int i = 0; i < axis.length; i++) {
      TrackingServoData x = new TrackingServoData(axis[i]);
      x.axis = axis[i];
      x.servoControl = (Servo) createPeer(axis[i]);
      servoControls.put(axis[i], x);
      servoControls.get(axis[i]).servoControl.setPin(pins[i]);
      servoControls.get(axis[i]).servoControl.attach(controller, pins[i]);
      // use the output min/max for the pid output i guess?  TODO: what should the output range be set to??
      pid.setOutputRange(axis[i], -x.servoControl.getMaxInput(), x.servoControl.getMaxInput());
      // TODO: parameterize this better!  connect method is too smart for it's own good.
      //pid.setOutputRange(axis[i], -5, 5);
      x.servoControl.moveTo(x.servoControl.getRest() + 2);
      x.currentServoPos = x.servoControl.getPos();
      log.info("Attached to servo {} current pos {}", x.name, x.currentServoPos);
    }
    opencv = (OpenCV) createPeer("opencv");
    opencv.setCameraIndex(cameraIndex);
    // opencv.addListener("publishOpenCVData", getName(), "onOpenCVData");
    subscribe(opencv.getName(), "publishOpenCVData");
    LKOpticalTrackFilterName = String.format("%s.%s", opencv.getName(), FILTER_LK_OPTICAL_TRACK);
    // TODO - think of a "validate" method
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
    meta.addDescription("uses a video input and vision library to visually track objects");
    meta.addCategory("vision", "video", "sensor", "control");
    meta.addPeer("x", "Servo", "pan servo");
    meta.addPeer("y", "Servo", "tilt servo");
    meta.addPeer("pid", "Pid", "Pid service - for all your pid needs");
    meta.addPeer("opencv", "OpenCV", "Tracking OpenCV instance");
    meta.addPeer("controller", "Arduino", "Tracking Arduino instance");
    return meta;
  }

  public void attach(ServoControl servo, String axis) {
    if (!(axis.equals("x") || axis.equals("y"))) {
      log.info("Axis must be x or y");
      return;
    }
    TrackingServoData tsd  = new TrackingServoData(axis);
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
    LKOpticalTrackFilterName = String.format("%s.%s", opencv.getName(), FILTER_LK_OPTICAL_TRACK);
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
      boolean useVirtualArduino = true;
      int xPin = 9;
      int yPin = 6;
      String arduinoPort = "COM5";
      int cameraIndex = 0;
      String frameGrabberType = "org.myrobotlab.opencv.SarxosFrameGrabber";

      /*
       * <pre> 1. setting frame grabber type does not update gui correctly
       * Sarxos => OpenCV :P </pre>
       */

      /*
       * Pid pid = (Pid)Runtime.start("pid", "Pid"); Servo pan =
       * (Servo)Runtime.start("x", "Servo"); Servo tilt =
       * (Servo)Runtime.start("y", "Servo"); OpenCV video =
       * (OpenCV)Runtime.start("opencv", "OpenCV"); Ssc32UsbServoController
       * controller = (Ssc32UsbServoController)Runtime.start("controller",
       * "Ssc32UsbServoController");
       */
      Runtime.start("gui", "SwingGui");
      // Runtime.start("webgui", "WebGui");

      if (useVirtualArduino) {
        VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
        virtual.connect(arduinoPort);
      }

      Tracking t01 = (Tracking) Runtime.start("t01", "Tracking");
      ServoControl x = t01.getX();
      // x.setInverted(true);

      ServoControl y = t01.getY();
      // y.setInverted(true);

      t01.connect(arduinoPort, xPin, yPin, cameraIndex);
      OpenCV opencv = t01.getOpenCV();
      opencv.captureFromImageFile("resource/OpenCV/testData/ryan.jpg");
      opencv.broadcastState();


      //t01.startLKTracking();
      OpenCVFilterFaceRecognizer fr=(OpenCVFilterFaceRecognizer)t01.faceDetect(true);
      fr.train();
     
      //opencv.stopCapture();
      //opencv.captureFromImageFile("resource/OpenCV/testData/rachel.jpg");
     // opencv.broadcastState();
      //t01.faceDetect(true);
      // Runtime.start("python", "Python");

      // tracker.startLKTracking();
      /*
       * tracker.trackPoint(); tracker.faceDetect(); tracker.findFace();
       */

      /*
       * pan.setPin(6); tilt.setPin(7);
       * 
       * 
       * controller.attach(pan); controller.attach(tilt);
       * 
       * tracker.attach(video); tracker.attach(pan, tilt); tracker.attach(pid);
       * 
       * pid.attach(pan); pid.attach(tilt); pid.setPID("tracker.pid", 2.0, 5.0,
       * 1.0);
       * 
       * Runtime.start("gui", "SwingGui");
       * 
       * tracker.connect(arduinoPort, xPin, yPin, cameraIndex);
       */
      // tracker.startLKTracking();

      // tracker.getGoodFeatures();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
