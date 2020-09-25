package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Registration;

public interface ServiceLifeCycle {

  public Registration registered(Registration registration);

  public String created(String serviceName);

  public String started(String serviceName);

  public String stopped(String serviceName);

  public String released(String serviceName);

}
