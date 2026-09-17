package org.myrobotlab.jme3;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;

/**
 * Ray-pick helpers for the OAK-D chest overlay in JMonkeyEngine. Closest
 * visible hit wins; HUD helpers (frustum, markers, floor grid) are skipped so
 * a click on the depth mesh can be published as an IK goal.
 */
public final class DepthPick {

  public static final String USER_DATA_KEY = "mrl.depthPick";

  public static final String DEPTH_CLOUD = "chest.depthCloud";

  public static final String DEPTH_RGB_MESH = "chest.depthRgbMesh";

  /** World-space InMoov left-hand reach overlay (not an OAK-D pick target). */
  public static final String LEFT_HAND_REACH = "_marker.leftHandReach";

  public static final class Hit {
    public final Geometry geometry;
    public final Vector3f world;
    public final boolean depthOverlay;

    public Hit(Geometry geometry, Vector3f world, boolean depthOverlay) {
      this.geometry = geometry;
      this.world = world;
      this.depthOverlay = depthOverlay;
    }
  }

  private DepthPick() {
  }

  public static void mark(Spatial spatial) {
    if (spatial != null) {
      spatial.setUserData(USER_DATA_KEY, true);
    }
  }

  public static boolean isDepthOverlay(Spatial spatial) {
    if (spatial == null) {
      return false;
    }
    Object flag = spatial.getUserData(USER_DATA_KEY);
    if (Boolean.TRUE.equals(flag)) {
      return true;
    }
    return isDepthOverlayName(spatial.getName());
  }

  public static boolean isDepthOverlayName(String name) {
    if (name == null) {
      return false;
    }
    return name.contains("depthRgbMesh") || name.contains("depthCloud");
  }

  public static boolean isSkip(Spatial spatial) {
    if (spatial == null || isCulled(spatial)) {
      return true;
    }
    return isSkipName(spatial.getName());
  }

  public static boolean isSkipName(String name) {
    if (name == null) {
      return false;
    }
    if (name.startsWith("_marker.") || name.endsWith(".frustum") || name.contains("depthMeter")) {
      return true;
    }
    if ("wireframe grid".equals(name) || "depthHud".equals(name)) {
      return true;
    }
    return name.contains("BoundingBox") || name.endsWith(".bb");
  }

  public static boolean isCulled(Spatial spatial) {
    Spatial s = spatial;
    while (s != null) {
      if (s.getCullHint() == CullHint.Always) {
        return true;
      }
      s = s.getParent();
    }
    return false;
  }

  /**
   * Closest collision that is visible and not a helper overlay. Sorted by
   * {@link CollisionResults#getCollision(int)}.
   */
  public static Hit firstPick(CollisionResults results) {
    if (results == null) {
      return null;
    }
    for (int i = 0; i < results.size(); i++) {
      CollisionResult cr = results.getCollision(i);
      Geometry g = cr.getGeometry();
      if (isSkip(g)) {
        continue;
      }
      Vector3f pt = cr.getContactPoint();
      if (pt == null) {
        continue;
      }
      return new Hit(g, pt, isDepthOverlay(g));
    }
    return null;
  }
}
