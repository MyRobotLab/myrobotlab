package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.repo.ServiceArtifact;
import org.myrobotlab.framework.repo.ServiceDependency;
import org.myrobotlab.framework.repo.ServiceExclude;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * list of relations from a Service type to a Dependency key the key is used to
 * look up in the masterList - this keeps the data normalized and if one Service
 * fulfills its dependency and the dependency is shared with another Service
 * type, it is fulfilled there too
 * 
 * The dependency key is the "org" - no version is keyed at the moment.. this
 * would be something to avoid anyway (complexities of cross-versions - jar
 * hell)
 * 
 */
public class ServiceType implements Serializable, Comparator<ServiceType> {

  transient public final static Logger log = LoggerFactory.getLogger(ServiceType.class);

  private static final long serialVersionUID = 1L;

  /**
   * available in the UI(s)
   */
  Boolean available = true; // why not ? :P
  transient public Set<String> categories = new HashSet<String>();
  /**
   * dependency keys of with key structure {org}-{version}
   */
  public List<ServiceDependency> dependencies = new ArrayList<ServiceDependency>();
  /**
   * description of what the service does
   */
  String description = null;
  Boolean includeServiceInOneJar = false;
  Boolean isCloudService = false;
  
  // only used for appending ServiceExcludes to ServiceDependencies
  transient private ServiceDependency lastDependency;
  
  String license;// = "Apache";
  String link;
  String name;
  public Map<String, ServiceReservation> peers = new TreeMap<String, ServiceReservation>();

  Boolean requiresKeys = false;

  String simpleName;

  /**
   * the single sponsor of this service
   */
  String sponsor;

  /**
   * service life-cycle state 
   * inactive | created | registered | running | stopped | released
   */
  String state = null;

  /**
   * what is left TODO on this service for it to be ready for release
   */
  String todo;
  
  Integer workingLevel = null;
  public ServiceType() {
  }

  public ServiceType(Class<?> clazz) {
    this.name = clazz.getCanonicalName();
    this.simpleName = clazz.getSimpleName();
  }

  public ServiceType(String name) {
    this.name = name;
    this.simpleName = name.substring(name.lastIndexOf(".") + 1);
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

  public void addPeer(String name, String peerType, String comment) {
    // peers.put(name, new ServiceReservation(name, peerType, comment));
    mergePeer(new ServiceReservation(name.trim(), peerType.trim(), comment));
  }

  public void addPeer(String name, String peerType, String comment, boolean autoStart) {
    mergePeer(new ServiceReservation(name.trim(), peerType.trim(), comment, autoStart));
  }

  public void addRootPeer(String actualName, String peerType, String comment) {
    peers.put(actualName, new ServiceReservation(actualName, actualName, peerType, comment, true, true));
  }

  public void addTodo(String todo) {
    this.todo = todo;
  }

  @Override
  public int compare(ServiceType o1, ServiceType o2) {
    return o1.name.compareTo(o2.name);
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

  public String getName() {
    return name;
  }

  public Map<String, ServiceReservation> getPeers() {
    return peers;
  }

  /*
   * public void setReady(boolean b) { this.ready = b; }
   */

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

  /**
   * checks if already exists - if it does - merges only unset values into peers
   * 
   * @param sr
   *          the service reservation
   */
  public void mergePeer(ServiceReservation sr) {
    if (peers.containsKey(sr.key)) {
      ServiceReservation existing = peers.get(sr.key);
      existing.actualName = (existing.actualName != null) ? existing.actualName : sr.actualName;
      existing.fullTypeName = (existing.fullTypeName != null) ? existing.fullTypeName : sr.fullTypeName;
      existing.comment = (existing.comment != null) ? existing.comment : sr.comment;
    } else {
      peers.put(sr.key, sr);
    }
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

  /**
   * sharing means sharePeer is forced - while addPeer will check before adding
   * 
   * @param key
   *          k
   * @param actualName
   *          n
   * @param peerType
   *          n
   * @param comment
   *          comment
   */
  public void sharePeer(String key, String actualName, String peerType, String comment) {
    peers.put(key, new ServiceReservation(key, actualName, peerType, comment));
  }

  public int size() {
    return dependencies.size();
  }

  @Override
  public String toString() {
    return name;
  }

  public ServiceReservation getPeer(String peerKey) {
    return peers.get(peerKey);
  }

}
