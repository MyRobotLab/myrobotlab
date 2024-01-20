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
  
}
