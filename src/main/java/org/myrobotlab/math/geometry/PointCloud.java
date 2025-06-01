package org.myrobotlab.math.geometry;

/**
 * TODO - add more meta data ?
 * 
 * @author GroG
 *
 */
public class PointCloud {
  int width = 0;
  int height = 0;
  // FloatBuffer data = null;
  Point3df[] data;
  float[] colors;

  public PointCloud(Point3df[] data) {
    this.data = data;
  }

  public Point3df[] getData() {
    return data;
  }

  public void setColors(float[] colors) {
    this.colors = colors;
  }

  public float[] getColors() {
    return colors;
  }

}
