package org.myrobotlab.service;

import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.headtracking.OculusTracking;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.oculus.OculusDisplay;
import org.myrobotlab.opencv.OpenCVFilterAffine;
import org.myrobotlab.opencv.OpenCVFilterTranspose;
import org.myrobotlab.opencv.OpenCVFilterUndistort;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.service.config.OculusRiftConfig;
import org.myrobotlab.service.data.Orientation;
import org.myrobotlab.service.interfaces.PointPublisher;
import org.slf4j.Logger;

import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.HmdDesc;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.TrackingState;

/**
 * The OculusRift service for MyRobotLab.
 * 
 * Currently this service only exposed the head tracking information from the
 * rift. The Yaw, Pitch and Roll are exposed. Yaw - twist around vertical axis
 * (look left/right) Pitch - twist around horizontal axis (look up/down) Roll -
 * twist around axis in front of you (tilt head left/right)
 * 
 * Coming soon, lots of great stuff...
 * 
 * @author kwatters
 *
 */
// TODO: implement publishOculusRiftData ...
public class OculusRift extends Service<OculusRiftConfig>  implements PointPublisher {

  public static final int ABS_TIME_MS = 0;
  public static final boolean LATENCY_MARKER = false;

  public static final String RIGHT_OPEN_CV = "rightOpenCV";
  public static final String LEFT_OPEN_CV = "leftOpenCV";
  private static final long serialVersionUID = 1L;
  private static final float RAD_TO_DEGREES = 57.2957795F;
  public final static Logger log = LoggerFactory.getLogger(OculusRift.class);

  // the Rift stuff.
  transient protected Hmd hmd;

  private boolean initialized = false;
  transient private RiftFrame lastRiftFrame = new RiftFrame();

  transient private OpenCVFilterAffine leftAffine = new OpenCVFilterAffine("leftAffine");
  transient private OpenCVFilterAffine rightAffine = new OpenCVFilterAffine("rightAffine");

  private boolean calibrated = false;
  // Two OpenCV services, one for the left eye, one for the right eye.
  transient public OpenCV leftOpenCV;
  transient public OpenCV rightOpenCV;
  transient private OculusDisplay display;

  // TODO: make these configurable...
  private int leftCameraIndex = 0;
  private int rightCameraIndex = 1;

  // TODO: encapsulate the configuration and calibration of the rift
  public float leftCameraDx = 0;
  public float leftCameraDy = 0;
  public float leftCameraAngle = 0;

  public float rightCameraDx = 0;
  public float rightCameraDy = 0;
  public float rightCameraAngle = 180;

  public String leftEyeURL = null;
  public String rightEyeURL = null;

  public String frameGrabberType = "OpenCV";
  public String cvInputSource = null;

  transient private HmdDesc hmdDesc;

  transient public OculusTracking headTracker = null;

  // for single camera support, mirror the images
  private boolean mirrorLeftCamera = true;

  public static class RiftFrame {
    public SerializableImage left;
    public SerializableImage right;
  }

  public OculusRift(String n, String id) {
    super(n, id);
  }

  // Boradcast the state of the peers to notify the gui.
  @Override
  public Service broadcastState() {
    // notify the gui
    if (leftOpenCV != null)
      leftOpenCV.broadcastState();
    if (rightOpenCV != null)
      rightOpenCV.broadcastState();
    return this;
  }

  private void setupRift() {

    // Initalize the JNA library/ head mounted device.
    Hmd.initialize();
    // TODO: is this delay needed?
    try {
      Thread.sleep(400);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
    hmd = Hmd.create();
    if (null == hmd) {
      throw new IllegalStateException("Unable to initialize HMD");
    }
    hmdDesc = hmd.getDesc();
    // hmd.configureTracking();
    // TODO: ?? what happened to configure tracking call?
    hmd.recenterPose();
    // hmd.initialize();

    log.info("Created HMD Oculus Rift Sensor and configured tracking.");
  }

  public void initContext() {

    if (!initialized) {
      log.info("Init the rift.");
      // Init the rift..
      setupRift();

      initialized = true;
      // now that we have the hmd. lets start up the polling thread.
      headTracker = new OculusTracking(hmd, hmdDesc);
      headTracker.oculus = this;
      headTracker.start();
      log.info("Started head tracking thread.");

      // create and start the open cv services
      // TODO: start via runtime?
      // leftOpenCV = new OpenCV(getName() + "." + LEFT_OPEN_CV);
      initLeftOpenCV();

      // start the right eye
      if (!mirrorLeftCamera) {
        initRightOpenCV();
      }

      boolean addUndistort = true;
      if (addUndistort) {
        addUndistortFilter();
      }

      // if the cameras are mounted at 90 degrees rotation, transpose the
      // image data to flip the resolution.
      boolean addTransposeEyes = false;
      if (addTransposeEyes) {
        // left eye
        addTransposeFilter();
      }

      // OpenCVFilterResize leftResizeFilter = new
      // OpenCVFilterResize("lrf");
      // TODO: resize the image resolution so it works with the rift.
      // int w = display.getWidth()/2;
      // int h = display.getHeight();
      // leftResizeFilter.setDestHeight(h);
      // leftResizeFilter.setDestWidth(w);

      // OpenCVFilterResize rightResizeFilter = new
      // OpenCVFilterResize("rrf");
      // rightResizeFilter.setDestHeight(h);
      // rightResizeFilter.setDestWidth(w);
      // leftOpenCV.addFilter(leftResizeFilter);
      // rightOpenCV.addFilter(rightResizeFilter);

      // configure the affine filters to calibrate image position and
      // rotation.
      // leftAffine.setDx(200);
      addAffineFilter();

      // start the left camera.
      capture();

      // TODO: handle the "end of the pipeline" as the input source.
      boolean addYolo = false;
      if (addYolo) {
        addYoloFilter();
      }

      // Now turn on the camras.
      // set camera index
      // Now that the Rift and OpenCV has been setup.
      // we should wait for the camera to start up.

      display = new OculusDisplay();
      // on publish frame we'll update the current frame in the rift..
      // synchronization issues maybe?
      // Ok, we never return here! that's not good. this should be it's
      // own
      // thread.
      // display.run();
      display.oculus = this;
      display.setHmd(hmd);
      display.start();
      log.info("Oculus display started and running.");
    } else {
      log.info("Rift interface already initialized.");
    }
  }

  public void addAffineFilter() {
    leftAffine.setDx(leftCameraDx);
    leftAffine.setDy(leftCameraDy);
    leftAffine.setAngle(leftCameraAngle);
    // the affine is always on top i guess
    leftOpenCV.addFilter(leftAffine);
    // leftOpenCV.setDisplayFilter("left");
    if (!mirrorLeftCamera) {
      rightAffine.setDx(rightCameraDx);
      rightAffine.setDy(rightCameraDy);
      rightAffine.setAngle(rightCameraAngle);
      rightOpenCV.addFilter(rightAffine);
    }
  }

  public void capture() {
    leftOpenCV.capture();
    if (!mirrorLeftCamera) {
      rightOpenCV.capture();
    }
  }

  public void addYoloFilter() {
    OpenCVFilterYolo yoloLeft = new OpenCVFilterYolo("left");
    leftOpenCV.addFilter(yoloLeft);
    leftOpenCV.setDisplayFilter("left");
    if (!mirrorLeftCamera) {
      OpenCVFilterYolo yoloRight = new OpenCVFilterYolo("right");
      // TODO: consider what it means to add this to both eyes.
      rightOpenCV.addFilter(yoloRight);
      rightOpenCV.setDisplayFilter("right");
    }
  }

  public void addTransposeFilter() {
    OpenCVFilterTranspose t1 = new OpenCVFilterTranspose("t1");
    t1.flipCode = 1;
    leftOpenCV.addFilter(t1);
    // right eye
    if (!mirrorLeftCamera) {
      OpenCVFilterTranspose t2 = new OpenCVFilterTranspose("t2");
      t2.flipCode = 1;
      rightOpenCV.addFilter(t2);
    }
  }

  public void addUndistortFilter() {
    OpenCVFilterUndistort ud1 = new OpenCVFilterUndistort("ud1");
    leftOpenCV.addFilter(ud1);
    if (!mirrorLeftCamera) {
      OpenCVFilterUndistort ud2 = new OpenCVFilterUndistort("ud2");
      rightOpenCV.addFilter(ud2);
    }
  }

  public void initRightOpenCV() {
    String serviceName = getName() + "." + RIGHT_OPEN_CV;
    // rightOpenCV = new OpenCV(serviceName);
    rightOpenCV = (OpenCV) Runtime.start(serviceName, "OpenCV");
    rightOpenCV.startService();
    // TODO: remove me this is a work around for opencv
    rightOpenCV.setCameraIndex(rightCameraIndex);
    if (frameGrabberType != null) {
      rightOpenCV.setGrabberType(frameGrabberType);
    }
    if (rightEyeURL != null) {
      rightOpenCV.setInputFileName(rightEyeURL);
      rightOpenCV.setInputSource(OpenCV.INPUT_SOURCE_FILE);
    }
    if (cvInputSource != null) {
      rightOpenCV.setInputSource(cvInputSource);
    }
    subscribe(rightOpenCV.getName(), "publishDisplay");
  }

  public void initLeftOpenCV() {
    leftOpenCV = (OpenCV) Runtime.start(getName() + "." + LEFT_OPEN_CV, "OpenCV");
    leftOpenCV.startService();
    // TODO: remove me this is a work around for opencv
    leftOpenCV.setCameraIndex(leftCameraIndex);
    if (frameGrabberType != null) {
      leftOpenCV.setGrabberType(frameGrabberType);
    }
    if (cvInputSource != null) {
      leftOpenCV.setInputSource(cvInputSource);
    }
    if (leftEyeURL != null) {
      leftOpenCV.setInputFileName(leftEyeURL);
      leftOpenCV.setInputSource(OpenCV.INPUT_SOURCE_FILE);
    }
    subscribe(leftOpenCV.getName(), "publishDisplay");
  }

  public void updateAffine() {
    // this method will update the angle / dx / dy settings on the affine
    // filters.
    log.info("Update left affine");
    leftAffine.setDx(leftCameraDx);
    leftAffine.setDy(leftCameraDy);
    leftAffine.setAngle(leftCameraAngle);
    if (!mirrorLeftCamera) {
      log.info("Update right affine");
      rightAffine.setDx(rightCameraDx);
      rightAffine.setDy(rightCameraDy);
      rightAffine.setAngle(rightCameraAngle);
    }

  }

  public void onDisplay(SerializableImage frame) {

    // if we're only one camera
    // the left frame is both frames.
    if (mirrorLeftCamera) {
      // if we're mirroring the left camera
      // log.info("Oculus Frame Source {}",frame.getSource());
      if ("leftAffine".equals(frame.getSource())) {
        lastRiftFrame.left = frame;
        lastRiftFrame.right = frame;
      }
    } else {
      if ("left".equals(frame.getSource())) {
        lastRiftFrame.left = frame;
      } else if ("leftAffine".equals(frame.getSource())) {
        lastRiftFrame.left = frame;
      } else if ("right".equals(frame.getSource())) {
        lastRiftFrame.right = frame;
      } else if ("rightAffine".equals(frame.getSource())) {
        lastRiftFrame.right = frame;
      } else {
        log.error("unknown source {}", frame.getSource());
      }
    }

    if (!calibrated) {
      if (leftAffine.getLastClicked() != null && rightAffine.getLastClicked() != null) {
        // calibrate!
        double deltaY = (leftAffine.getLastClicked().getY() - rightAffine.getLastClicked().getY()) / 2.0;
        leftAffine.setDy(-deltaY);
        rightAffine.setDy(deltaY);
        calibrated = true;
        log.info("Calibrated images! DeltaY = {}", deltaY);
      }
    }

    // update the oculus display with the last rift frame
    if (display != null) {
      display.setCurrentFrame(lastRiftFrame);
    } else {
      // TODO: wait on the display to be initialized ?
      // maybe just log something?
      // log.warn("The Oculus Display was null.");
    }
    invoke("publishRiftFrame", lastRiftFrame);
  }

  @Override
  public void stopService() {
    super.stopService();
    // TODO: validate proper life cycle.
    if (headTracker != null) {
      headTracker.stop();
    }
    if (hmd != null) {
      hmd.destroy();
      Hmd.shutdown();
    }
  }

  /**
   * Resets orientation of the head tracking Makes the current orientation the
   * straight ahead orientation. Use this to align your perspective.
   */
  public void resetSensor() {
    // hmd.
    if (initialized) {
      // ?
      hmd.recenterPose();
      // hmd.resetSensor();
    } else {
      log.info("Sensor not initalized.");
    }
  }

  /**
   * Log the head tracking info to help with debugging.
   */
  public void logOrientation() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    OvrVector3f position = trackingState.HeadPose.Pose.Position;
    position.x *= 100.0f;
    position.y *= 100.0f;
    position.z *= 100.0f;
    log.info((int) position.x + ", " + (int) position.y + " " + (int) position.z);

    // TODO: see if we care about this value ?
    // float w = trackingState.HeadPose.Pose.Orientation.w;
    float x = trackingState.HeadPose.Pose.Orientation.x;
    float y = trackingState.HeadPose.Pose.Orientation.y;
    float z = trackingState.HeadPose.Pose.Orientation.z;

    log.info("Roll: " + z * RAD_TO_DEGREES);
    log.info("Pitch:" + x * RAD_TO_DEGREES);
    log.info("Yaw:" + y * RAD_TO_DEGREES);
  }

  public float getYaw() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    float y = trackingState.HeadPose.Pose.Orientation.y * RAD_TO_DEGREES;
    return y;
  }

  public float getRoll() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    float z = trackingState.HeadPose.Pose.Orientation.z * RAD_TO_DEGREES;
    return z;
  }

  public float getPitch() {
    TrackingState trackingState = hmd.getTrackingState(ABS_TIME_MS, LATENCY_MARKER);
    float x = trackingState.HeadPose.Pose.Orientation.x * RAD_TO_DEGREES;
    return x;
  }

  public void addRiftFrameListener(Service service) {
    addListener("publishRiftFrame", service.getName(), "onRiftFrame");
  }

  public RiftFrame publishRiftFrame(RiftFrame frame) {
    return frame;
  }

  public int getLeftCameraIndex() {
    return leftCameraIndex;
  }

  public void setLeftCameraIndex(int leftCameraIndex) {
    this.leftCameraIndex = leftCameraIndex;
  }

  public int getRightCameraIndex() {
    return rightCameraIndex;
  }

  public void setRightCameraIndex(int rightCameraIndex) {
    this.rightCameraIndex = rightCameraIndex;
  }

  public Orientation publishOrientation(Orientation data) {
    // grab the last published data (if we need it somewhere)
    // if (data != null) {
    // System.out.println("Oculus Data: " + data.toString());
    // }
    // TODO: make this a proper callback / subscribe..
    if (display != null) {
      display.updateOrientation(data);
    }
    // return the data to the mrl framework to be published.
    return data;
  }

  @Override
  public List<Point> publishPoints(List<Point> points) {
    // TODO Auto-generated method stub
    return points;
  }

  // This publishes the hand position and orientation from the oculus touch
  // controllers.
  public Point publishLeftHandPosition(Point point) {
    //
    return point;
  }

  public Point publishRightHandPosition(Point point) {
    //
    return point;
  }

  public static void main(String s[]) {
    // LoggingFactory.init("INFO");

    // org.apache.log4j.BasicConfigurator.configure();
    // LoggingFactory.getInstance().setLevel(Level.INFO);
    LoggingFactory.init("INFO");

    Runtime.createAndStart("gui", "SwingGui");
    // Runtime.createAndStart("python", "Python");
    OculusRift rift = (OculusRift) Runtime.createAndStart("oculus", "OculusRift");

    String leftEyeURL = "http://10.0.0.2:8080/?action=stream";
    String rightEyeURL = "http://10.0.0.2:8081/?action=stream";

    // rift.setLeftEyeURL(leftEyeURL);
    // rift.setRightEyeURL(rightEyeURL);

    rift.leftCameraAngle = 0;
    rift.leftCameraDy = 5;
    rift.rightCameraDy = -5;
    // call this once you've updated the affine stuff?
    rift.updateAffine();

    rift.initContext();

    rift.logOrientation();

    rift.leftOpenCV.capture();
    // TODO: configuration to enable left/right camera roll tracking.
    // while (true) {
    // float roll = rift.getRoll();
    // rift.leftAffine.setAngle(-roll+180);
    // rift.rightAffine.setAngle(-roll);
    // try {
    // Thread.sleep(1);
    // } catch (InterruptedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // break;
    // }
    // }

  }

  public String getLeftEyeURL() {
    return leftEyeURL;
  }

  public void setLeftEyeURL(String leftEyeURL) {
    this.leftEyeURL = leftEyeURL;
  }

  public String getRightEyeURL() {
    return rightEyeURL;
  }

  public void setRightEyeURL(String rightEyeURL) {
    this.rightEyeURL = rightEyeURL;
  }

  public String getFrameGrabberType() {
    return frameGrabberType;
  }

  public void setGrabberType(String frameGrabberType) {
    this.frameGrabberType = frameGrabberType;
  }

  public float getLeftCameraDx() {
    return leftCameraDx;
  }

  public void setLeftCameraDx(float leftCameraDx) {
    this.leftCameraDx = leftCameraDx;
  }

  public float getLeftCameraDy() {
    return leftCameraDy;
  }

  public void setLeftCameraDy(float leftCameraDy) {
    this.leftCameraDy = leftCameraDy;
  }

  public float getLeftCameraAngle() {
    return leftCameraAngle;
  }

  public void setLeftCameraAngle(float leftCameraAngle) {
    this.leftCameraAngle = leftCameraAngle;
  }

  public float getRightCameraDx() {
    return rightCameraDx;
  }

  public void setRightCameraDx(float rightCameraDx) {
    this.rightCameraDx = rightCameraDx;
  }

  public float getRightCameraDy() {
    return rightCameraDy;
  }

  public void setRightCameraDy(float rightCameraDy) {
    this.rightCameraDy = rightCameraDy;
  }

  public float getRightCameraAngle() {
    return rightCameraAngle;
  }

  public void setRightCameraAngle(float rightCameraAngle) {
    this.rightCameraAngle = rightCameraAngle;
  }

  public boolean isMirrorLeftCamera() {
    return mirrorLeftCamera;
  }

  public void setMirrorLeftCamera(boolean mirrorLeftCamera) {
    this.mirrorLeftCamera = mirrorLeftCamera;
  }

  public String getCvInputSource() {
    return cvInputSource;
  }

  public void setCvInputSource(String cvInputSource) {
    this.cvInputSource = cvInputSource;
  }

}
