package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * 
 * ServiceLifeCycleListener listens to the following life cycle events
 *
 */
public interface ServiceLifeCyclePublisher extends NameProvider {

  public String created(String name) ;

  public Registration registered(Registration registration);

  public String started(String name);

  public String stopped(String name);

  public String released(String name) ;
  
  // public void attachServiceLifeCycleListener(ServiceLifeCyclePublisher service);
  public void attachServiceLifeCycleListener(String name);

}
