package org.myrobotlab.service.data;

import java.io.Serializable;
import java.util.Objects;

/**
 * A very basic POJO to transport scripts. Used in Python and Py4j and their
 * front ends, and for saving or updating scripts.
 * 
 * @author GroG
 *
 */
public class Script implements Serializable {

  static final long serialVersionUID = 1L;
  /**
   * unique location &amp; key of the script e.g. /mrl/scripts/myScript.py
   */
  public String file;
  /**
   * actual code/contents of the script
   */
  public String code;

  /**
   * Convenient constructor
   * 
   * @param file
   * @param script
   */
  public Script(String file, String script) {
    this.file = file;
    this.code = script;
  }

  @Override
  public int hashCode() {
    return Objects.hash(file, code);
  }

  @Override
  public String toString() {
    return "Script{" + "file='" + file + '\'' + ", code='" + code + '\'' + '}';
  }

}
