package org.myrobotlab.boofcv;

import org.myrobotlab.cv.CvFilter;
import org.myrobotlab.service.BoofCV;

import boofcv.struct.image.ImageBase;

public abstract class BoofCVFilter implements CvFilter {

  protected String name;
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  protected boolean enabled = true;
  transient BoofCV boofcv = null;
  
  public BoofCVFilter(String name) {
    this.name = name;
  }
  
  @Override
  public void disable() {
    enabled = false;
  }

  @Override
  public void enable() {
    enabled = true;
  }
  
  @Override
  public boolean isEnabled() {
    return enabled;
  }
  
  
  public void setBoofCV(BoofCV boofcv) {
    this.boofcv = boofcv;
  }
  
  public abstract ImageBase<?> process(ImageBase<?> image) throws InterruptedException;

  // override if necessary
  public void release() {
  }
  
}
