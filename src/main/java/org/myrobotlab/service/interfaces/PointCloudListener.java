package org.myrobotlab.service.interfaces;

import org.myrobotlab.math.geometry.PointCloud;

/**
 * Receives a point cloud from a {@link PointCloudPublisher}.
 * <p>
 * A publisher invokes {@code publishPointCloud}, and MyRobotLab delivers each
 * cloud here as {@link #onPointCloud(PointCloud)}.
 * {@link org.myrobotlab.service.OpenCV} and
 * {@link org.myrobotlab.service.OakD} publish clouds, and
 * {@link org.myrobotlab.service.JMonkeyEngine} renders them. Unless the
 * publisher documents another frame, points are camera-frame meters as
 * described on {@link PointCloud}: X right, Y down, Z forward.
 */
public interface PointCloudListener {

  /**
   * Accept one point cloud.
   *
   * @param pointCloud
   *          points for this sample. A {@code null} cloud means there is
   *          nothing to render.
   */
  void onPointCloud(PointCloud pointCloud);
}
