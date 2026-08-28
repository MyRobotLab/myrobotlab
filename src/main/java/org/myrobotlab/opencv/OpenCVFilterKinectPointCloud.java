package org.myrobotlab.opencv;

/**
 * Alias for {@link OpenCVFilterDepthToPointCloud} so
 * {@code cv.addFilter("points", "KinectPointCloud")} still resolves.
 */
public class OpenCVFilterKinectPointCloud extends OpenCVFilterDepthToPointCloud {

  private static final long serialVersionUID = 1L;

  public OpenCVFilterKinectPointCloud() {
    super();
  }

  public OpenCVFilterKinectPointCloud(String name) {
    super(name);
  }
}
