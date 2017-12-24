package org.myrobotlab.io;

import java.io.InputStream;
import java.io.OutputStream;

public class StreamIO {

  /**
   * general purpose stream closer for single line closing
   * @param is the input stream
   */
  static final public void close(InputStream is) {
    try {
      if (is != null) {
        is.close();
      }
    } catch (Exception e) {
    }
  }

  static final public void close(InputStream in, OutputStream out) {
    close(in);
    close(out);
  }

  static final public void close(OutputStream is) {
    try {
      if (is != null) {
        is.close();
      }
    } catch (Exception e) {
    }
  }

}
