package org.myrobotlab.headtracking;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OculusRift;
import org.myrobotlab.service.data.Orientation;
import org.slf4j.Logger;

import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.HmdDesc;
import com.oculusvr.capi.PoseStatef;
import com.oculusvr.capi.TrackingState;

/**
 * OculusHeadTracking -This is a helper thread that will poll the oculus head
 * tracking information and it will publish the roll/pitch/yaw information.
 *
 */
public class OculusTracking implements Runnable, Serializable {

  public final static Logger log = LoggerFactory.getLogger(OculusTracking.class);
  private static final long serialVersionUID = -4067064437788846187L;
  protected final Hmd hmd;
  protected final HmdDesc hmdDesc;
  boolean running = false;
  transient public OculusRift oculus;
  transient Thread trackerThread = null;
  private int pollIntervalMS = 20;

  private long frameCount = 0;
  private int downSample = 50;

  public OculusTracking(Hmd hmd, HmdDesc hmdDesc) {
    // Grab a handle to the initialized hmd.
    this.hmd = hmd;
    this.hmdDesc = hmdDesc;
  }

  @Override
  public void run() {
    running = true;
    while (running) {

      frameCount++;
      TrackingState trackingState = hmd.getTrackingState(pollIntervalMS, false);

      // TODO: do we care about "w" ?
      // double w = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.w);
      // rotations about x axis (pitch)
      double pitch = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.x);
      // rotation about y axis (yaw)
      double yaw = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.y);
      // rotation about z axis (roll)
      double roll = Math.toDegrees(trackingState.HeadPose.Pose.Orientation.z);

      Orientation headTrackingData = new Orientation(roll, pitch, yaw);
      oculus.invoke("publishOrientation", headTrackingData);

      // positional information.
      double x = trackingState.HeadPose.Pose.Position.x;
      double y = trackingState.HeadPose.Pose.Position.y;
      double z = trackingState.HeadPose.Pose.Position.z;

      ArrayList<Point> points = new ArrayList<Point>();
      points.add(new Point(x, y, z, roll, pitch, yaw));
      oculus.invoke("publishPoints", points);

      PoseStatef[] hands = trackingState.HandPoses;
      if (hands.length > 0) {
        // System.out.println(hands.length);
        PoseStatef leftHand = hands[1];
        PoseStatef rightHand = hands[0];

        x = leftHand.Pose.Position.x;
        y = leftHand.Pose.Position.y;
        z = leftHand.Pose.Position.z;
        pitch = Math.toDegrees(leftHand.Pose.Orientation.x);
        // rotation about y axis (yaw)
        yaw = Math.toDegrees(leftHand.Pose.Orientation.y);
        // rotation about z axis (roll)
        roll = Math.toDegrees(leftHand.Pose.Orientation.z);

        x = rightHand.Pose.Position.x;
        y = rightHand.Pose.Position.y;
        z = rightHand.Pose.Position.z;
        pitch = Math.toDegrees(rightHand.Pose.Orientation.x);
        // rotation about y axis (yaw)
        yaw = Math.toDegrees(rightHand.Pose.Orientation.y);
        // rotation about z axis (roll)
        roll = Math.toDegrees(rightHand.Pose.Orientation.z);

        Point leftHandPoint = new Point(x, y, z, roll, pitch, yaw);
        Point rightHandPoint = new Point(x, y, z, roll, pitch, yaw);

        ArrayList<Point> handPoints = new ArrayList<Point>();
        handPoints.add(leftHandPoint);
        handPoints.add(rightHandPoint);
        // publish left and hand positions. This is pretty much raw data
        // we need to scale it / translate it rotate it.. etc..
        // probably best done inside of the ik service.

        // need to down sample this
        if (frameCount % downSample == 0) {
          oculus.invoke("publishLeftHandPosition", leftHandPoint);
          oculus.invoke("publishRightHandPosition", rightHandPoint);
        }
      }

      try {
        // There need to be polling interval here.
        Thread.sleep(pollIntervalMS);
      } catch (InterruptedException e) {
        e.printStackTrace();
        // break out ...
        break;
      }

      // TODO: can I get oculus touch tracking info?

    }
  }

  public boolean isRunning() {
    return running;
  }

  public void setRunning(boolean running) {
    this.running = running;
  }

  public OculusRift getOculus() {
    return oculus;
  }

  public void setOculus(OculusRift oculus) {
    this.oculus = oculus;
  }

  public void start() {
    log.info("starting head tracking");
    if (trackerThread != null) {
      log.info("Head tracker thread already started.");
      return;
    }
    trackerThread = new Thread(this, String.format("%s_oculusHeadTracking", oculus.getName()));
    trackerThread.start();
  }

  public void stop() {
    log.debug("stopping head tracking");
    running = false;
    trackerThread = null;
  }

  // default 20 ms
  public int getPollIntervalMS() {
    return pollIntervalMS;
  }

  public void setPollIntervalMS(int pollIntervalMS) {
    this.pollIntervalMS = pollIntervalMS;
  }

}
