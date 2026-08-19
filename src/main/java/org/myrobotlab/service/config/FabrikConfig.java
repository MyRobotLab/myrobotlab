package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.kinematics.JointFrame;
import org.myrobotlab.kinematics.Point;

/**
 * FABRIK inverse kinematics configuration. Meters, Y-up, matching
 * JMonkeyEngine. {@link #joints} is the measured chain; empty means use the
 * arm's default geometry until "Calibrate from simulator" is pressed.
 */
public class FabrikConfig extends ServiceConfig {

  public static class JointCalibration {

    public String name;

    public String rotationMask;

    public double originX;

    public double originY;

    public double originZ;

    public double axisX;

    public double axisY;

    public double axisZ = 1.0;

    public double angleDeg;

    public double servoMin = 0.0;

    public double servoMax = 180.0;

    public double angleAtServoMin = 0.0;

    public double angleAtServoMax = 180.0;
  }

  public boolean worldFrame = true;

  public List<JointCalibration> joints = new ArrayList<>();

  public double endEffectorX = 0.0;

  public double endEffectorY = 0.0;

  public double endEffectorZ = 0.0;

  public boolean endEffectorSet = false;

  public double originX = 0.0;

  public double originY = 0.0;

  public double originZ = 0.0;

  public void fromJointFrames(List<JointFrame> frames) {
    joints = new ArrayList<>();
    if (frames == null) {
      return;
    }
    for (JointFrame frame : frames) {
      if (frame == null || frame.origin == null || frame.axis == null) {
        continue;
      }
      JointCalibration jc = new JointCalibration();
      jc.name = frame.name;
      jc.rotationMask = frame.rotationMask;
      jc.originX = frame.origin.getX();
      jc.originY = frame.origin.getY();
      jc.originZ = frame.origin.getZ();
      jc.axisX = frame.axis.getX();
      jc.axisY = frame.axis.getY();
      jc.axisZ = frame.axis.getZ();
      jc.angleDeg = frame.angleDeg;
      jc.servoMin = frame.servoMin;
      jc.servoMax = frame.servoMax;
      jc.angleAtServoMin = frame.angleAtServoMin;
      jc.angleAtServoMax = frame.angleAtServoMax;
      joints.add(jc);
    }
  }

  public List<JointFrame> toJointFrames() {
    List<JointFrame> frames = new ArrayList<>();
    if (joints == null) {
      return frames;
    }
    for (JointCalibration jc : joints) {
      if (jc == null) {
        continue;
      }
      JointFrame frame = new JointFrame(jc.name, new Point(jc.originX, jc.originY, jc.originZ), new Point(jc.axisX, jc.axisY, jc.axisZ));
      frame.rotationMask = jc.rotationMask;
      frame.angleDeg = jc.angleDeg;
      frame.withServoMap(jc.servoMin, jc.servoMax, jc.angleAtServoMin, jc.angleAtServoMax);
      frames.add(frame);
    }
    return frames;
  }
}
