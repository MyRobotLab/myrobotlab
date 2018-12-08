package org.myrobotlab.math.geometry;

/**
 * TODO - a lot more meta data probably
 * @author GroG
 *
 */
public class PointCloud {
  Point3df[][] viewPortData = null;
  
  public PointCloud(Point3df[][] points) {    
    viewPortData = points;
  }
  
  public int getViewPortX() {
    if (viewPortData != null) {
      return viewPortData.length;
    }
    return 0;
  }
  
  public int getViewPortY() {
    if (viewPortData != null) {
      if (viewPortData.length > 0) {
        return viewPortData[0].length;
      }
    }
    return 0;
  }

  public void set(Point3df[][] data) {
    viewPortData = data;
  }
  
  public Point3df[][] getViewPort(){
    return viewPortData;
  }
}
