package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.math.geometry.PointCloud;

/**
 * Publishes a point cloud on {@code publishPointCloud}.
 * <p>
 * MyRobotLab sends the returned cloud to each attached
 * {@link PointCloudListener#onPointCloud(PointCloud)}.
 * Implementations return the cloud listeners should receive.
 * The service name comes from {@link NameProvider#getName()}.
 */
public interface PointCloudPublisher extends NameProvider {

  /**
   * Publish one point cloud.
   *
   * @param pointCloud
   *          points to send
   * @return the cloud delivered to listeners, typically {@code pointCloud}
   */
  PointCloud publishPointCloud(PointCloud pointCloud);
}
