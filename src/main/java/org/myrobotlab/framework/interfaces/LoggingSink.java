package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Status;

public interface LoggingSink extends NameProvider {

  public Status error(String format, Object... args);

  public Status error(Exception e);

  public Status info(String format, Object... args);

  public Status warn(String format, Object... args);
}
