package org.myrobotlab.framework;

public class MRLException extends Exception {

  private static final long serialVersionUID = 1L;

  /*
   * public MRLException(String msg) { super(msg); }
   */

  public MRLException(String format, Object... params) {
    super(String.format(format, params));
  }

}
