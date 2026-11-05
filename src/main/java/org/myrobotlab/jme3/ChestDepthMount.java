package org.myrobotlab.jme3;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Orientation of the chest depth camera on a VinMoov joint.
 * <p>
 * {@code topStom} is authored about 3.8° nose-up. The visible chest
 * ({@code torso.full}) is a child that cancels that pitch, so the plate is
 * level with the horizon. A camera parented to the joint with an identity
 * rotation inherits the nose-up axis and the depth cone points above the
 * horizon. This mount copies the plate, then applies an optional pitch / yaw /
 * roll in the plate frame. Pitch 0 stays level with the chest.
 */
public final class ChestDepthMount {

  private ChestDepthMount() {
  }

  /**
   * Local rotation for a camera node parented to {@code joint}.
   *
   * @param exclude
   *          the camera node itself, so it is not chosen as the plate
   */
  public static Quaternion localRotation(Node joint, Spatial exclude, float pitchDeg, float yawDeg, float rollDeg) {
    // Do not call updateGeometricState() here. On a node that is already in the
    // scene that walk clears the subtree's light refresh, so the chest meshes
    // under topStom render with no lights (black, textures gone). World
    // rotation below uses checkDoTransformUpdate(), which only refreshes
    // transforms.
    Quaternion plate = plateLocalRotation(joint, exclude);
    Quaternion configured = new Quaternion().fromAngles(pitchDeg * FastMath.DEG_TO_RAD, yawDeg * FastMath.DEG_TO_RAD,
        rollDeg * FastMath.DEG_TO_RAD);
    return plate.mult(configured);
  }

  /**
   * Child of the joint that stands more upright than the joint itself — the
   * mesh pivot that cancels the bind pitch. Otherwise a heading-only rotation
   * that puts the optical axis on the horizon.
   */
  static Quaternion plateLocalRotation(Node joint, Spatial exclude) {
    if (joint == null) {
      return new Quaternion();
    }
    float jointUp = joint.getWorldRotation().mult(Vector3f.UNIT_Y).y;
    Quaternion best = null;
    float bestUp = jointUp;
    for (Spatial child : joint.getChildren()) {
      if (child == exclude || isDepthRig(child)) {
        continue;
      }
      float up = child.getWorldRotation().mult(Vector3f.UNIT_Y).y;
      if (up > bestUp + 1e-3f) {
        bestUp = up;
        best = child.getLocalRotation();
      }
    }
    if (best != null) {
      return best.clone();
    }
    return levelWithHorizon(joint);
  }

  /**
   * Local rotation that keeps the parent's heading and lays the optical axis
   * on the world horizon (world up, no roll).
   */
  static Quaternion levelWithHorizon(Node joint) {
    Quaternion parentWorld = joint.getWorldRotation();
    Vector3f forward = parentWorld.mult(Vector3f.UNIT_Z);
    forward.y = 0f;
    if (forward.lengthSquared() < 1e-8f) {
      forward = parentWorld.mult(Vector3f.UNIT_X);
      forward.y = 0f;
    }
    if (forward.lengthSquared() < 1e-8f) {
      forward = new Vector3f(0f, 0f, 1f);
    } else {
      forward.normalizeLocal();
    }
    Vector3f up = Vector3f.UNIT_Y;
    Vector3f right = up.cross(forward).normalizeLocal();
    Quaternion world = new Quaternion().fromAxes(right, up, forward);
    return parentWorld.inverse().mult(world);
  }

  private static boolean isDepthRig(Spatial child) {
    String n = child.getName();
    if (n == null) {
      return false;
    }
    return n.contains("depthCamera") || n.contains("depthCloud") || n.contains("depthRgb") || n.contains("depthMeter")
        || n.endsWith(".frustum");
  }
}
