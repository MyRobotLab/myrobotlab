package org.myrobotlab.framework;

public class MrlException extends Exception {

  private static final long serialVersionUID = 1L;

  public MrlException(String format, Object... params) {
    super(String.format(format, params));
  }

}
