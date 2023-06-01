package org.myrobotlab.service.data;

import java.io.Serializable;

public class Script2 implements Serializable {
  static final long serialVersionUID = 1L;
  /**
   * unique location &amp; key of the script e.g. /mrl/scripts/myScript.py
   */
  public String file;
  /**
   * actual code/contents of the script
   */
  public String code;

  public Script2(String file, String script) {
    this.file = file;
    this.code = script;
  }

}
