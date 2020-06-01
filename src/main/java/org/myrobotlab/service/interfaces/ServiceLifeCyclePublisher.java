package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Registration;

public interface ServiceLifeCyclePublisher {
  
  public Registration registered(Registration registration);
  
  public String released(String serviceName);

}
