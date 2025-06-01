package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * 
 * ServiceLifeCycleListener listens to the following life cycle events
 *
 */
public interface ServiceLifeCyclePublisher extends NameProvider {

  public String created(String name);

  public Registration registered(Registration registration);

  public String started(String name);

  public String stopped(String name);

  public String released(String name);

  default public void attachServiceLifeCycleListener(String name) {
    addListener("registered", name);
    addListener("created", name);
    addListener("started", name);
    addListener("stopped", name);
    addListener("released", name);
  }

  default public void detachServiceLifeCycleListener(String name) {

    removeListener("registered", name);
    removeListener("created", name);
    removeListener("started", name);
    removeListener("stopped", name);
    removeListener("released", name);
  }

  public void addListener(String localTopic, String otherService);

  public void removeListener(String topicMethod, String callbackName);

}
