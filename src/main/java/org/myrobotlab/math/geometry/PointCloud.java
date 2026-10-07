package org.myrobotlab.math.geometry;

/**
 * Camera-frame point cloud (X right, Y down, Z forward, meters) unless a
 * publisher documents otherwise. See {@link org.myrobotlab.service.data.DepthFrame}.
 */
public class PointCloud {
  int width = 0;
  int height = 0;
  Point3df[] data;
  float[] colors;

  public PointCloud(Point3df[] data) {
    this.data = data != null ? data : new Point3df[0];
  }

  public Point3df[] getData() {
    return data;
  }

  public void setData(Point3df[] data) {
    this.data = data;
  }

  public void setColors(float[] colors) {
    this.colors = colors;
  }

  public float[] getColors() {
    return colors;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public int size() {
    return data == null ? 0 : data.length;
  }

}
