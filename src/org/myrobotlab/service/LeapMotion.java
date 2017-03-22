package org.myrobotlab.service;

import java.io.IOException;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.leap.LeapMotionListener;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.LeapData;
import org.myrobotlab.service.interfaces.LeapDataListener;
import org.myrobotlab.service.interfaces.LeapDataPublisher;
import org.myrobotlab.service.interfaces.PointPublisher;
import org.slf4j.Logger;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Vector;

public class LeapMotion extends Service implements LeapDataListener, LeapDataPublisher, PointPublisher {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LeapMotion.class);

  transient LeapMotionListener listener = null;

  transient Controller controller = new Controller();

  public LeapData lastLeapData = null;

  public LeapMotion(String n) {
    super(n);
  }

  public void activateDefaultMode() {
    controller.setPolicyFlags(Controller.PolicyFlag.POLICY_DEFAULT);
    log.info("default mode active");
    return;
  }

  public void activateVRMode() {
    controller.setPolicyFlags(Controller.PolicyFlag.POLICY_OPTIMIZE_HMD);
    log.info("virtual reality mode active");
    return;
  }

  public void addFrameListener(Service service) {
    addListener("publishFrame", service.getName(), "onFrame");
  }

  public void addLeapDataListener(Service service) {
    addListener("publishLeapData", service.getName(), "onLeapData");
  }

  public void checkPolicy() {
    log.info("controller.policyFlags()");
  }

  /**
   * Return the angle of the finger for the hand specified This computes the
   * angle based on the dot product of the palmNormal and the fingerDirection
   * Theta = arccos( (V1.V2) / ( |V1| * |V2| )
   * 
   * @param hand
   *          - "left" or "right"
   * @param tip
   *          - 0 (thumb) / 1 (index) .. etc..
   * @return angle in degrees
   */
  public double getJointAngle(String hand, Integer tip) {
    com.leapmotion.leap.Hand h = null;
    if ("left".equalsIgnoreCase(hand)) {
      // left hand
      h = controller.frame().hands().leftmost();
    } else {
      // right hand
      h = controller.frame().hands().rightmost();
    }
    // TODO: does this return the correct finger?
    Finger f = h.fingers().get(tip);
    Vector palmNormal = h.palmNormal();
    Vector fDir = f.direction();
    // TODO: validate that this is what we actually want.
    // otherwise we can directly compute the angleTo in java.
    float angleInRadians = palmNormal.angleTo(fDir);
    // convert to degrees so it's easy to pass to servos
    double angle = Math.toDegrees(angleInRadians);
    return angle;
  }

  public float getLeftStrength() {
    Frame frame = controller.frame();
    com.leapmotion.leap.Hand hand = frame.hands().leftmost();
    float strength = hand.grabStrength();
    return strength;
  }

  public float getRightStrength() {
    Frame frame = controller.frame();
    com.leapmotion.leap.Hand hand = frame.hands().rightmost();
    float strength = hand.grabStrength();
    return strength;
  }

  @Override
  public LeapData onLeapData(LeapData data) {

    return data;
    // TODO Auto-generated method stub

  }

  public Controller publishConnect(Controller controller) {
    return controller;
  }

  public Controller publishDisconnect(Controller controller) {
    return controller;
  }

  public Controller publishExit(Controller controller) {
    return controller;
  }

  public Frame publishFrame(Frame frame) {
    return frame;
  }

  public Controller publishInit(Controller controller) {
    return controller;
  }

  @Override
  public LeapData publishLeapData(LeapData data) {
    // if (data != null) {
    // log.info("DATA" + data.leftHand.posX);
    // }
    return data;
  }

  @Override
  public void startService() {
    super.startService();
    listener = new LeapMotionListener(this);
    // we've been asked to start.. we should start tracking !
    this.startTracking();
  }

  public void startTracking() {
    controller.addListener(listener);
  }

  public void stopTracking() {
    controller.removeListener(listener);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {

      LeapMotion leap = new LeapMotion("leap");
      leap.startService();
      Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");

      // Have the sample listener receive events from the controller

      // leap.startTracking();
      // Keep this process running until Enter is pressed
      log.info("Press Enter to quit...");
      try {
        System.in.read();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // Remove the sample listener when done
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public List<Point> publishPoints(List<Point> points) {
    return points;
  }

  public void addPointsListener(Service s) {
    // TODO - reflect on a public heard method - if doesn't exist error ?
    addListener("publishPoints", s.getName(), "onPoints");
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

    ServiceType meta = new ServiceType(LeapMotion.class.getCanonicalName());
    meta.addDescription("Leap Motion Service");
    meta.addCategory("sensor");
    meta.addDependency("com.leapmotion", "2.1.3");
    return meta;
  }

}
