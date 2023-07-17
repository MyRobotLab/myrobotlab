package org.myrobotlab.cv;

import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * 
 * @author GroG
 *
 */
public interface ComputerVision extends NameProvider {

  void stopCapture();

  void capture();

  CvFilter addFilter(String name, String filterType);

  void removeFilter(String name);

  void removeFilters();

  void enableFilter(String name);

  void disableFilter(String name);

  void disableAll();

  Integer setCameraIndex(Integer index);

  void setDisplayFilter(String name);
}
