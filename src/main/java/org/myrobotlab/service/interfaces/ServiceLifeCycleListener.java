package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Registration;

/**
 * 
 * ServiceLifeCycleListener listens to the following 
 * life cycle events
 *
 */
public interface ServiceLifeCycleListener {

  public void onCreated(String serviceName);
  
  public void onRegistered(Registration registration);

  public void onStarted(String serviceName);

  public void onStopped(String serviceName);
  
  public void onReleased(String serviceName);

}
