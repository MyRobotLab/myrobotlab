package org.myrobotlab.kinematics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;

/**
 * Forward-kinematics samples of an arm's palm, voxel-binned into a point cloud
 * for the simulator. Joint limits come from {@link JointFrame} servo ranges.
 *
 * <p>
 * Coordinates are world meters, Y-up — the same frame as JMonkeyEngine and the
 * IK solvers. The cloud is the set of palm positions the chain can actually
 * reach, not a Cartesian bounding box.
 * </p>
 */
public final class ReachCloud {

  /** Servo samples per joint. 8^4 = 4096 FK evaluations for an InMoov arm. */
  public static final int DEFAULT_STEPS = 8;

  /** Merge palms that land in the same cell so the overlay reads as a volume. */
  public static final float DEFAULT_VOXEL_M = 0.025f;

  /** Cyan — distinct from the blue left-hand marker and the green IK goal. */
  public static final float[] COLOR = { 0.15f, 0.82f, 0.95f, 0.42f };

  private ReachCloud() {
  }

  public static PointCloud sample(List<JointFrame> frames, Point endEffector, int stepsPerJoint, float voxelM) {
    FabrikArm arm = new FabrikArm("reach");
    if (frames == null || endEffector == null || !arm.applyJointFrames(frames, endEffector)) {
      return empty();
    }
    return sample(arm, stepsPerJoint, voxelM);
  }

  public static PointCloud sample(FabrikArm arm, int stepsPerJoint, float voxelM) {
    if (arm == null || arm.getNumJoints() == 0) {
      return empty();
    }
    return voxelize(arm.samplePalmWorkspace(stepsPerJoint), voxelM);
  }

  public static PointCloud voxelize(List<Point> palms, float voxelM) {
    float v = voxelM <= 1e-6f ? DEFAULT_VOXEL_M : voxelM;
    Map<Long, Point3df> cells = new LinkedHashMap<>();
    if (palms != null) {
      for (Point p : palms) {
        if (p == null) {
          continue;
        }
        int ix = (int) Math.floor(p.getX() / v);
        int iy = (int) Math.floor(p.getY() / v);
        int iz = (int) Math.floor(p.getZ() / v);
        long key = cellKey(ix, iy, iz);
        if (!cells.containsKey(key)) {
          cells.put(key, new Point3df((ix + 0.5f) * v, (iy + 0.5f) * v, (iz + 0.5f) * v));
        }
      }
    }
    Point3df[] data = cells.values().toArray(new Point3df[0]);
    PointCloud cloud = new PointCloud(data);
    float[] colors = new float[data.length * 4];
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(COLOR, 0, colors, i * 4, 4);
    }
    cloud.setColors(colors);
    return cloud;
  }

  static long cellKey(int ix, int iy, int iz) {
    return (ix + 524288L) | ((iy + 524288L) << 20) | ((iz + 524288L) << 40);
  }

  private static PointCloud empty() {
    return new PointCloud(new Point3df[0]);
  }
}
