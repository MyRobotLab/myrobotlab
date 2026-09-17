package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.math.geometry.PointCloud;

public interface PointCloudPublisher extends NameProvider {

  PointCloud publishPointCloud(PointCloud pointCloud);
}
