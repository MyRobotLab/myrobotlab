package org.myrobotlab.opencv;

/**
 * Alias for {@link OpenCVFilterDepthToPointCloud} so
 * {@code cv.addFilter("points", "KinectPointCloud")} still resolves.
 */
public class OpenCVFilterKinectPointCloud extends OpenCVFilterDepthToPointCloud {
  /**
   * Catalog metadata for the WebGui filter guide. Static so it can be read
   * without constructing the filter (constructors may start services).
   */
  public static OpenCVFilterInfo catalogInfo() {
    return new OpenCVFilterInfo("KinectPointCloud",
        "Alias of DepthToPointCloud: converts 16-bit depth (mm) into a 3D point cloud using Kinect-like intrinsics, plus a colorized depth preview.",
        "Use a depth grabber. Tune fx/fy/cx/cy, stride, and min/max depth. Subscribe to published PointCloud from the OpenCV service.",
        OpenCVFilterInfo.DEP_KINECT);
  }

  @Override
  public OpenCVFilterInfo getFilterInfo() {
    return catalogInfo();
  }


  private static final long serialVersionUID = 1L;

  public OpenCVFilterKinectPointCloud() {
    super();
  }

  public OpenCVFilterKinectPointCloud(String name) {
    super(name);
  }
}
