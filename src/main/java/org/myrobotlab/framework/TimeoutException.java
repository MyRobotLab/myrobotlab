package org.myrobotlab.framework;

import org.checkerframework.checker.formatter.qual.FormatMethod;

public class TimeoutException extends Exception {

  private static final long serialVersionUID = 1L;

  @FormatMethod
  public TimeoutException(String format, Object... params) {
    super(String.format(format, params));
  }

}
