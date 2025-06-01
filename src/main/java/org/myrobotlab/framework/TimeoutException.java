package org.myrobotlab.framework;

public class TimeoutException extends Exception {

  private static final long serialVersionUID = 1L;

  public TimeoutException(String format, Object... params) {
    super(String.format(format, params));
  }

}
