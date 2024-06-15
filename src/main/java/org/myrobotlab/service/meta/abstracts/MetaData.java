package org.myrobotlab.service.meta.abstracts;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
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
public class MetaData implements Serializable {

  transient private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MetaData.class);

  /**
   * available in the UI(s)
   */
  Boolean available = true; // why not ? :P

  /**
   * if this service's dependencies are currently installed
   */
  public boolean installed = false;

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


  // public Map<String, ServiceReservation> peers = new TreeMap<String, ServiceReservation>();

  /**
   * true if the service requires a key e.g. Polly
   */
  Boolean requiresKeys = false;

  /**
   * simple class name of this service
   */
  String simpleName;

  /**
   * the single sponsor of this service
   */
  String sponsor;

  /**
   * what is left TODO on this service for it to be ready for release
   */
  String todo;

  Integer workingLevel = null;

  static public String getConfigType(String type) {
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
    ServiceDependency library = new ServiceDependency(groupId, artifactId, version, ext, includeServiceInOneJar);
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
    return sb.toString();
  }
  
  


  public String getType() {
    return type;
  }

  @Deprecated /* use ServiceConfig */
  public Plan getDefault(String name) {

    // FIXME - plan passed in
    Plan plan = new Plan(name);
    try {

      // FIXME-sc read from 
      // either overwrite starting with base default
      // or invert - do not overwrite if override is supplied
      
      // gonna try - use first file only - don't seek override, never overwrite
      // 1. attempt to read data/config/blah/name.yml
      // 2. try resources/resource/Type/type.yml
      // 3. construct new ServiceConfig ?
      
      Class<?> c = Class.forName("org.myrobotlab.service.config." + simpleName + "Config");
      Constructor<?> con = c.getConstructor();
      ServiceConfig sc = (ServiceConfig) con.newInstance();

      // FIXME handle no Config object ... just Service
      plan.put(name, sc);

    } catch (Exception e) {
      log.info("could not find {} loading generalized ServiceConfig", type);
      ServiceConfig sc = new ServiceConfig();
      sc.type = simpleName;
      plan.put(name, sc);
    }
    return plan;
  }

  @Deprecated /* use ServiceConfig */
  public static MetaData get(String type) {
    try {
      type = getConfigType(type);
      Class<?> c = Class.forName(type);
      Constructor<?> con = c.getConstructor();
      return (MetaData) con.newInstance();

    } catch (Exception e) {
      log.error("getting MetaData failed on {}", type);
    }
    return null;
  }

}
