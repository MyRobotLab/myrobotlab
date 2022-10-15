package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * 
 * ServiceLifeCycleListener listens to the following life cycle events
 *
 */
public interface ServiceLifeCycleListener extends NameProvider {

  public void onCreated(String name);

  public void onRegistered(Registration registration);

  public void onStarted(String name);

  public void onStopped(String name);

  public void onReleased(String name);

  // public void attachServiceLifeCyclePublisher(ServiceLifeCyclePublisher
  // service);

}
