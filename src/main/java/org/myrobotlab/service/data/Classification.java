package org.myrobotlab.service.data;

import org.myrobotlab.math.geometry.Rectangle;

/**
 * A very simple classification POJO,
 * great for network definitions ;)
 * 
 * @author GroG
 *
 */
public class Classification {

  public long ts = System.currentTimeMillis();
  public String label = null;
  public Double confidence = 0.0;
  public Rectangle bbox = null;
  public String src = null;

  /**
   * Spatial position in <em>camera</em> frame meters (X right, Y down, Z
   * forward) when the detector provides it (OAK-D SpatialDetectionNetwork).
   */
  public Double x = null;
  public Double y = null;
  public Double z = null;

}
