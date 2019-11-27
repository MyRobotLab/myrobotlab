package org.myrobotlab.framework;

/**
 * 
 * @author GroG
 * 
 * Registration is sent when a processes wishes some or all of its services to be registered in another
 * process.  The act of registration is dependant on the registrar process.  Potentially some processes
 * will ignore registrations, however, some will create typed based subscriptions and build UIs from the
 * registration and allow remote access to the registered services.
 *
 */
public class Registration {
 
  public String id;
  public String name;
  public String typeKey;
  public ServiceType type;
  
  /**
   * current serialized state of the service - default encoding is json
   */
  public String state;
  
  public Registration(String id, String name, String typeKey, ServiceType type) {
    this.id = id;
    this.name = name;
    this.typeKey = typeKey;
    this.type = type;
  }
  
  public String toString() {
    return String.format("%s %s %s",  id, name, typeKey);
  }
}
