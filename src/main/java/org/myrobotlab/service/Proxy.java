package org.myrobotlab.service;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.google.gson.internal.LinkedTreeMap;

/**
 * A proxy for services in other languages (Javascript, Python, C++, Go, Node,
 * Lisp) defined and running outside of this Jvm. Potentially, if the service
 * complies with convention - a proxy will be created and the services will be
 * able to communicate with each other.
 * 
 * The messaging conversation to establish connectivity is (will/be) well
 * structured.
 * 
 * Although other encodings and Apis are possible, this one follows the
 * "messaging api"
 * 
 * @author GroG
 *
 */
public class Proxy extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Proxy.class);

  public Proxy(String name, String id) {
    super(name, id);
  }
    public Proxy(String name, String id, String type) {
    super(name, id);
    
    // necessary for serialized transport
    this.id = id;
    serviceClass = type;
    int period = type.lastIndexOf(".");
    simpleName = type.substring(period);
    MethodCache cache = MethodCache.getInstance();
    
    // hmm interesting ....
    cache.cacheMethodEntries(this.getClass());

    try {
      serviceType = getMetaData(type);
    } catch (Exception e) {
      log.error("could not extract meta data for {}", type);
    }

    // FIXME - this is 'sort-of' static :P
    if (methodSet == null) {
      // FIXME - this is handled elsewhere (but very important)
      methodSet = getMessageSet();
    }

    if (interfaceSet == null) {
   // FIXME - this is handled elsewhere (but very important)
      interfaceSet = getInterfaceSet();
    }

    this.inbox = new Inbox(getFullName());
    this.outbox = new Outbox(this);
    Runtime.register(new Registration(this));
  }
  
  String type = null;
  
  LinkedTreeMap<String, Object> state = null;
  
  // pre-processor hook
  // overloaded invoke ?
  // methodMap data - dynaFuncs
  // overloaded serialization toJson ...
  
  public String getId() {
    return "unknown";
  }
  
  public boolean isLocal() {
    return false;
  }
  
  public String getType() {
    // overloaded to support other types
    return Proxy.class.getCanonicalName();
  }
  
  public void setState(String json) {
    state = CodecUtils.toTree(json);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Proxy.class);
    meta.addDescription("used as a general template");
    meta.setAvailable(false); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("general");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      Runtime.main(new String[] { "--interactive", "--id", "id"});
      Runtime.start("proxy", "Proxy");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /*
  public void setId(String id) {
    this.id = id;
  }*/

  public void setState(LinkedTreeMap<String, Object> state) {
    this.state = state;
  }

  /*
  public void setType(String type) {
      this.type = type;
  } */

}
