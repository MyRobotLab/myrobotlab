package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Registration;

/**
 * 
 * ServiceLifeCycleListener listens to the following life cycle events
 *
 */
public interface ServiceLifeCycleListener {

  public void onCreated(String fullname);

  public void onRegistered(Registration registration);

  public void onStarted(String fullname);

  public void onStopped(String fullname);

  public void onReleased(String fullname);

}
