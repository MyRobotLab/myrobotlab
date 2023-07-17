package org.myrobotlab.cv;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.myrobotlab.math.geometry.PointCloud;

/**
 * Computer Vision Data an agnostic data type to allow data to be shared between
 * services
 * 
 * @author GroG
 *
 */
public abstract class CVData implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final String POINT_CLOUDS = "point.cloud";
  public static final String INPUT = "input";
  public static final String OUTPUT = "output";

  public abstract Set<String> getKeySet();

  public abstract List<PointCloud> getPointCloudList();

  public abstract PointCloud getPointCloud();

  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

}
