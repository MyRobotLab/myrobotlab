package org.myrobotlab.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

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
   * The list of interfaces that a service being registered implements.
   * This list must contain the fully qualified names of Java interfaces,
   * and is only used for proxy generation. When generating proxies,
   * this list must contain at least the fully qualified name of ServiceInterface.
   */
  public List<String> interfaces = List.of();

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
  

  public Registration(String id, String name, String typeKey, ArrayList<String> interfaces) {
    this.id = id;
    this.name = name;
    this.typeKey = typeKey;
    this.interfaces = interfaces;
  }

  public Registration(ServiceInterface service) {
    log.info("creating registration for {}@{} - {}", service.getName(), service.getId(), service.getTypeKey());
    this.id = service.getId();
    this.name = service.getName();
    this.typeKey = service.getTypeKey();
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
