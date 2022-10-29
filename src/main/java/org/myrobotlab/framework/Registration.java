package org.myrobotlab.framework;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * 
 * @author GroG
 * 
 *         Registration is sent when a processes wishes some or all of its
 *         services to be registered in another process. The act of registration
 *         is dependant on the registrar process. Potentially some processes
 *         will ignore registrations, however, some will create typed based
 *         subscriptions and build UIs from the registration and allow remote
 *         access to the registered services.
 *
 */
public class Registration {

  transient final static Logger log = LoggerFactory.getLogger(Registration.class);

  protected String id;
  protected String name;
  protected String typeKey;

  /**
   * current serialized state of the service - default encoding is json for all
   * remote registration
   */
  public String state;

  /**
   * in process reference to a service - for internal use only - always null
   * remote
   */
  transient public ServiceInterface service = null;

  public Registration(String id, String name, String typeKey) {
    this.id = id;
    this.name = name;
    this.typeKey = typeKey;
  }

  public Registration(ServiceInterface service) {
    log.info("creating registration for {}@{} - {}", service.getName(), service.getId(), service.getType());
    this.id = service.getId();
    this.name = service.getName();
    this.typeKey = service.getType();
    // when this registration is re-broadcasted to remotes it will use this
    // serialization to init state
    this.state = CodecUtils.toJson(service);
    // if this is a local registration - need reference to service
    this.service = service;
  }

  @Override
  public String toString() {
    return String.format("%s %s %s", id, name, typeKey);
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public boolean isLocal(String id2) {
    return id.equals(id2);
  }

  public String getTypeKey() {
    return typeKey;
  }

  public String getState() {
    return state;
  }

  public String getFullName() {
    return String.format("%s@%s", name, id);
  }

  public boolean hasInterface(Class<?> interfaze) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Registration that = (Registration) o;
    return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(typeKey, that.typeKey) && Objects.equals(state, that.state) && Objects.equals(service, that.service);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, typeKey, state, service);
  }
}
