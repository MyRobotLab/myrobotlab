package org.myrobotlab.service.meta.abstracts;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.framework.repo.ServiceArtifact;
import org.myrobotlab.framework.repo.ServiceDependency;
import org.myrobotlab.framework.repo.ServiceExclude;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ArduinoConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

/**
 * MetaData describes most of the data about a service that is static. It's
 * dependencies, categories and other meta information.
 * 
 * It also describes its list of peers. They define a list of group this service
 * expects to interact with. When a service is started it gets a copy of
 * MetaData based on its instance name. When a new instance is created a list of
 * ServiceData.overrides are consulted, to allow the user to override actual
 * name and type information.
 * 
 */
public abstract class MetaData implements Serializable {

  transient private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MetaData.class);

  /**
   * available in the UI(s)
   */
  Boolean available = true; // why not ? :P

  // final transient Plan plan;

  /**
   * Set of categories this service belongs to
   */
  transient public Set<String> categories = new HashSet<String>();

  /**
   * dependency keys of with key structure {org}-{version}
   */
  public List<ServiceDependency> dependencies = new ArrayList<ServiceDependency>();

  /**
   * description of what the service does
   */
  String description = null;

  /**
   * if true the dependency of this service are packaged in the build of
   * myrobotlab.jar
   */
  Boolean includeServiceInOneJar = false;

  /**
   * service requires an internet connection because some or all of its
   * functionality
   */
  Boolean isCloudService = false;

  /**
   * used for appending ServiceExcludes to the ServiceDependencies
   */
  transient private ServiceDependency lastDependency;

  /**
   * license of the service
   */
  String license;// = "Apache";

  /**
   * relevant site to the Service
   */
  String link;

  /**
   * full type name of the service
   */
  String type;

  /**
   * key'd structure of other services that are necessary for the correct
   * function of this service can be modified with overrides before starting
   * named instance of this service
   */
  public Map<String, ServiceReservation> peers = new TreeMap<String, ServiceReservation>();

  /**
   * true if the service requires a key e.g. Polly
   */
  Boolean requiresKeys = false;

  /**
   * instance name of service this MetaData belongs to e.g. "i01"
   */
  // String serviceName;

  /**
   * simple class name of this service
   */
  String simpleName;

  /**
   * the single sponsor of this service
   */
  String sponsor;

  /**
   * service life-cycle state inactive | created | registered | running |
   * stopped | released
   */
  String state = null;

  /**
   * what is left TODO on this service for it to be ready for release
   */
  String todo;

  Integer workingLevel = null;

  static public String getFullMetaTypeName(String type) {
    if (type.contains(".") && type.endsWith("Meta")) {
      return type;
    }

    if (!type.contains(".") && !type.endsWith("Meta")) {
      type = String.format("org.myrobotlab.service.meta.%sMeta", type);
    } else {
      int pos = type.lastIndexOf(".");
      String serviceTypeName = type.substring(pos + 1);
      type = type.substring(0, pos) + ".meta." + serviceTypeName + "Meta";
    }
    return type;
  }

  public MetaData() {

    // this.plan = new Plan(this);
    // name is the name this meta class respresents
    // in the case of ArduinoMeta - it represents the
    // org.myrobotlab.service.Arduino
    // this.serviceName = name;
    this.simpleName = getClass().getSimpleName().substring(0, getClass().getSimpleName().lastIndexOf("Meta"));
    this.type = "org.myrobotlab.service." + simpleName;
  }

  public void addArtifact(String orgId, String classifierId) {
    lastDependency.add(new ServiceArtifact(orgId, classifierId));
  }

  public void addCategory(String... categories) {
    for (int i = 0; i < categories.length; ++i) {
      this.categories.add(categories[i]);
    }
  }

  public void addDependency(String groupId, String artifactId) {
    addDependency(groupId, artifactId, null, null);
  }

  public void addDependency(String groupId, String artifactId, String version) {
    addDependency(groupId, artifactId, version, null);
  }

  public void addDependency(String groupId, String artifactId, String version, String ext) {
    ServiceDependency library = new ServiceDependency(groupId, artifactId, version, ext);
    lastDependency = library;
    dependencies.add(library);
  }

  public void addDescription(String description) {
    this.description = description;
  }

  public void addLicense(String license) {
    this.license = license;
  }

  public void addTodo(String todo) {
    this.todo = todo;
  }

  public void exclude(String groupId, String artifactId) {
    // get last dependency
    // dependencies
    if (lastDependency == null) {
      log.error("DEPENDENCY NOT DEFINED - CANNOT EXCLUDE");
    }
    lastDependency.add(new ServiceExclude(groupId, artifactId));
  }

  public List<ServiceDependency> getDependencies() {
    return dependencies;
  }

  public String getDescription() {
    return description;
  }

  public String getLicense() {
    return license;
  }

  public String getLink() {
    return link;
  }

  // FIXME - change to name ... change name to type
  // check for webgui breakage
  // public String getName() {
  // return serviceName;
  // }

  public Map<String, ServiceReservation> getPeers() {
    return peers;
  }

  public String getSimpleName() {
    return simpleName;
  }

  public boolean includeServiceInOneJar() {
    return includeServiceInOneJar;
  }

  public void includeServiceInOneJar(Boolean b) {
    includeServiceInOneJar = b;
  }

  public boolean isAvailable() {
    return available;
  }

  public boolean requiresKeys() {
    return requiresKeys;
  }

  public void setAvailable(boolean b) {
    this.available = b;
  }

  public void setCloudService(boolean b) {
    isCloudService = b;
  }

  public void setLicenseApache() {
    addLicense("apache");
  }

  public void setLicenseGplV3() {
    addLicense("gplv3");
  }

  public void setLicenseProprietary() {
    addLicense("proprietary");
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setRequiresKeys(boolean b) {
    requiresKeys = b;
  }

  public void setSponsor(String sponsor) {
    this.sponsor = sponsor;
  }

  public int size() {
    return dependencies.size();
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(String.format("\n%s\n", simpleName));

    for (ServiceReservation sr : peers.values()) {
      sb.append(sr).append("\n");
    }

    return sb.toString();
  }

  public ServiceReservation getPeer(String peerKey) {
    if (peers.get(peerKey) == null) {
      log.warn("{} not found in peer keys - possible keys follow:", peerKey);
      for (String key : peers.keySet()) {
        log.info(key);
      }
    }
    return peers.get(peerKey);
  }

  /**
   * typical adding of a service reservation .. the actual name is left null, so
   * that this template will dynamically generate peer names depending on the
   * parents name
   * 
   * @param key
   *          k
   * @param peerType
   *          p
   * @param comment
   *          c
   * 
   */
  public void addPeer(String key, String peerType, String comment) {
    peers.put(key, new ServiceReservation(key, null, peerType, comment));
  }

  public void addPeer(String key, String peerType) {
    peers.put(key, new ServiceReservation(key, null, peerType, null));
  }

  public void setPeer(String key, String peerType) {
    ServiceReservation sr = peers.get(key);
    if (sr != null) {
      sr.key = key;
      sr.type = peerType;
    } else {
      addPeer(key, peerType, "set by user");
    }
  }

  public void setGlobalPeer(String key, String name, String peerType) {
    setGlobalPeer(key, name, peerType, "set by user");
  }

  public void setGlobalPeer(String key, String name, String peerType, String comment) {
    ServiceReservation sr = peers.get(name);
    if (sr != null) {
      sr.actualName = name;
      sr.type = peerType;
      sr.comment = comment;
    } else {
      peers.put(key, new ServiceReservation(key, name, peerType, comment));
    }
  }

  public void addPeer(String key, String actualName, String peerType, String comment) {
    peers.put(key, new ServiceReservation(key, actualName, peerType, comment));
  }

  public String getPeerActualName(String peerKey) {

    // return local defined name
    ServiceReservation peer = peers.get(peerKey);
    if (peer != null) {
      if (peer.actualName != null) {
        return peer.actualName;
      }
    }
    return null;
  }

  public String getType() {
    return type;
  }

  /**
   * Dynamically gets a default "load" for runtime to execute !
   * 
   * @param name
   * @param type
   * @return
   */
  public static Plan getDefault(String name, String type, Boolean autoStart) {

    try {

      if (type == null) {
        log.error("getDefault(null)");
        return null;
      }

      type = getFullMetaTypeName(type);

      Class<?> c = Class.forName(type);
      Constructor<?> mc = c.getConstructor();
      MetaData meta = (MetaData) mc.newInstance();
      return meta.getDefault(name, autoStart);

      // FIXME - add runtime ? - or should this be available to the concrete
      // metadata ?

    } catch (Exception e) {
      log.error("getting meta data failed on {}", type, e);
    }

    return null;
  }

  
  public static Plan getDefault(String name, String type) {
    return getDefault(name, type, null);
  }
  
  public Plan getDefault(String name, Boolean autoStart) {
    
    if (autoStart == null) {
      autoStart = true;
    }    

    // LinkedHashMap<String, ServiceConfig> ret = new LinkedHashMap<>();
    Plan plan = new Plan(name);
    plan.putPeers(name, peers, autoStart);
    
    try {

      Class<?> c = Class.forName("org.myrobotlab.service.config." + simpleName + "Config");
      Constructor<?> con = c.getConstructor();
      ServiceConfig sc = (ServiceConfig) con.newInstance();
      sc.autoStart = autoStart;

      // FIXME handle no Config object ... just Service
      plan.put(name, sc);

    } catch (Exception e) {
      log.info("could not find {} loading generalized ServiceConfig", type);
      ServiceConfig sc = new ServiceConfig();
      sc.autoStart = autoStart;
      sc.type = simpleName;
      plan.put(name, sc);
    }

    
    // plan.setConfig(ret);
    // plan.merge(plan);

    return plan;
  }

  public static MetaData get(String type) {
    try {
      type = getFullMetaTypeName(type);
      Class<?> c = Class.forName(type);
      Constructor<?> con = c.getConstructor();
      return (MetaData) con.newInstance();

    } catch (Exception e) {
      log.error("getting MetaData failed on {}", type);
    }
    return null;
  }
/*
  protected ServiceConfig getConfig(String name, String peerKey, Boolean autoStart) {
    if (autoStart == null) {
      autoStart = true;
    }
    ServiceReservation sr = getPeer(peerKey);
    String actualName = null;
    if (sr == null) {
      log.error("%s key %s not found", name, peerKey);
      return null;
    }
    if (sr.actualName != null) {
      actualName = sr.actualName;
    } else {
      actualName = name + "." + peerKey;
    }
    return plan.getPeer(actualName, sr.type, autoStart);
  }

  protected Plan getPlan() {
    return plan;
  }

  protected ServiceConfig addConfig(String name, ServiceConfig config, Boolean autoStart) {
    if (autoStart == null) {
      autoStart = true;
    }
    config.autoStart = autoStart;
    if (!plan.containsKey(name)) {
      plan.addPeer(name, config);
      return config;
    } else {
      return plan.get(name);
    }
  }
  */

  protected void setPeerName(String key, String actualName) {
    // FIXME - do we bother to check if a peer exists or just make one? - we don't have type info ...
    // FIXME - do we bother to check if its already set ? (merge ???)
    ServiceReservation sr = peers.get(key);
    if (sr != null) {
      sr.actualName = actualName;
    } else {
      log.error("key {} does not for peer", key);
    }
  }

/*  
  public void removeConfig(String name) {
    plan.remove(name);
  }
*/  
}
