package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

public interface JsonInvoker {

  /**
   * No parameter method
   * @param method
   * @return
   */
  public Object invoke(String method);

  /**
   * Encoded parameters as a JSON String (encoded once!)
   * @param method
   * @param encodedParameters
   * @return
   */
  public Object invoke(String method, String encodedParameters);
  
}
