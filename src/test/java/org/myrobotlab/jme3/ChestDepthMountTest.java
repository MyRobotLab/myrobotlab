package org.myrobotlab.jme3;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class ChestDepthMountTest {

  /** VinMoov topStom bind pitch, degrees. The torso mesh cancels it. */
  private static final float JOINT_PITCH_DEG = -3.77f;

  @Test
  public void plateCancelsJointPitchSoCameraIsLevel() {
    Node joint = pitchedJoint();
    Node cam = attachCamera(joint);
    cam.setLocalRotation(ChestDepthMount.localRotation(joint, cam, 0f, 0f, 0f));
    joint.updateGeometricState();

    Vector3f forward = jointForward(cam);
    Vector3f up = cam.getWorldRotation().mult(Vector3f.UNIT_Y);
    assertEquals(0f, forward.y, 1e-4f);
    assertEquals(1f, forward.z, 1e-4f);
    assertEquals(1f, up.y, 1e-4f);
  }

  @Test
  public void cameraFollowsPlateWhenJointRolls() {
    Node joint = pitchedJoint();
    Node cam = attachCamera(joint);
    cam.setLocalRotation(ChestDepthMount.localRotation(joint, cam, 0f, 0f, 0f));
    Quaternion bind = joint.getLocalRotation().clone();
    Quaternion roll = new Quaternion().fromAngleAxis(20f * FastMath.DEG_TO_RAD, Vector3f.UNIT_Z);
    joint.setLocalRotation(bind.mult(roll));
    joint.updateGeometricState();

    Vector3f camUp = cam.getWorldRotation().mult(Vector3f.UNIT_Y);
    Vector3f plateUp = joint.getChild("i01.torso.full").getWorldRotation().mult(Vector3f.UNIT_Y);
    assertEquals(plateUp.x, camUp.x, 1e-4f);
    assertEquals(plateUp.y, camUp.y, 1e-4f);
    assertEquals(plateUp.z, camUp.z, 1e-4f);
  }

  @Test
  public void noPlateStillSitsOnTheHorizon() {
    Node joint = new Node("i01.torso.topStom");
    joint.setLocalRotation(new Quaternion().fromAngles(JOINT_PITCH_DEG * FastMath.DEG_TO_RAD, 0f, 0f));
    Node cam = new Node("i01.chest.depthCamera");
    joint.attachChild(cam);
    cam.setLocalRotation(ChestDepthMount.localRotation(joint, cam, 0f, 0f, 0f));
    joint.updateGeometricState();

    Vector3f forward = jointForward(cam);
    assertEquals(0f, forward.y, 1e-4f);
    assertEquals(1f, forward.z, 1e-4f);
  }

  private static Node pitchedJoint() {
    Node joint = new Node("i01.torso.topStom");
    joint.setLocalRotation(new Quaternion().fromAngles(JOINT_PITCH_DEG * FastMath.DEG_TO_RAD, 0f, 0f));
    Node plate = new Node("i01.torso.full");
    plate.setLocalRotation(new Quaternion().fromAngles(-JOINT_PITCH_DEG * FastMath.DEG_TO_RAD, 0f, 0f));
    joint.attachChild(plate);
    return joint;
  }

  private static Node attachCamera(Node joint) {
    Node cam = new Node("i01.chest.depthCamera");
    joint.attachChild(cam);
    return cam;
  }

  private static Vector3f jointForward(Node cam) {
    return cam.getWorldRotation().mult(Vector3f.UNIT_Z);
  }
}
