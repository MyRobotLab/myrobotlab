package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.NameProvider;

public interface StatusListener extends NameProvider {

  /**
   * @param status events from Repo when dependency resolution is attempted
   * 
   */
  public void onStatus(final Status status);

}
