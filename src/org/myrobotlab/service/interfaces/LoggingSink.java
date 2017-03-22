package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Status;

public interface LoggingSink {

  public String getName();

  public Status error(String format, Object... args);
  
  public Status error(Exception e);

  public Status info(String format, Object... args);

  public Status warn(String format, Object... args);
}
