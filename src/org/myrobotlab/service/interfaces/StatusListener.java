package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.NameProvider;

public interface StatusListener extends NameProvider {

  /**
   * Status events from Repo when dependency resolution is attempted
   * 
   * @param status
   * @return
   */
  public void onStatus(final Status status);

}
