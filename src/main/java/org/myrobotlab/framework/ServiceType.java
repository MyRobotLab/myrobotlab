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

  String name;
  String simpleName;
  String link;
  String license;// = "Apache";
  Boolean isCloudService = false;
  Boolean requiresKeys = false;
  Boolean includeServiceInOneJar = false;

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  String state = null;
  Integer workingLevel = null;
  /**
   * description of what the service does
   */
  String description = null;
  /**
   * the single sponsor of this service
   */
  String sponsor;
  /**
   * ready for release
   */
  // Boolean ready = false;

  /**
   * available in the UI(s)
   */
  Boolean available = true; // why not ? :P

  /**
   * what is left todo on this service for it to be ready for release
   */
  String todo;

  /**
   * dependency keys of with key structure {org}-{version}
   */
  public List<ServiceDependency> dependencies = new ArrayList<ServiceDependency>();
  transient public Set<String> categories = new HashSet<String>();
  public Map<String, ServiceReservation> peers = new TreeMap<String, ServiceReservation>();

  // only used for appending ServiceExcludes to ServiceDependencies
  transient private ServiceDependency lastDependency;

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

  @Override
  public int compare(ServiceType o1, ServiceType o2) {
    return o1.name.compareTo(o2.name);
  }

  public String getName() {
    return name;
  }

  public String getSimpleName() {
    return simpleName;
  }

  public boolean isAvailable() {
    return available;
  }

  public int size() {
    return dependencies.size();
  }

  @Override
  public String toString() {
    return name;
  }

  public void addDescription(String description) {
    this.description = description;
  }

  public void addCategory(String... categories) {
    for (int i = 0; i < categories.length; ++i) {
      this.categories.add(categories[i]);
    }
  }

  public void addPeer(String name, String peerType, String comment) {
    // peers.put(name, new ServiceReservation(name, peerType, comment));
    mergePeer(new ServiceReservation(name.trim(), peerType.trim(), comment));
  }

  public void addPeer(String name, String peerType, String comment, boolean autoStart) {
    mergePeer(new ServiceReservation(name.trim(), peerType.trim(), comment, autoStart));
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

  public void addRootPeer(String actualName, String peerType, String comment) {
    peers.put(actualName, new ServiceReservation(actualName, actualName, peerType, comment, true, true));
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

  public void setAvailable(boolean b) {
    this.available = b;
  }

  public List<ServiceDependency> getDependencies() {
    return dependencies;
  }

  public Map<String, ServiceReservation> getPeers() {
    return peers;
  }

  public void setSponsor(String sponsor) {
    this.sponsor = sponsor;
  }

  /*
   * public void setReady(boolean b) { this.ready = b; }
   */

  public void addTodo(String todo) {
    this.todo = todo;
  }

  public String getDescription() {
    return description;
  }

  public void addLicense(String license) {
    this.license = license;
  }

  public String getLicense() {
    return license;
  }

  public void setLicenseProprietary() {
    addLicense("proprietary");
  }

  public void setLicenseApache() {
    addLicense("apache");
  }

  public void setLicenseGplV3() {
    addLicense("gplv3");
  }

  public void setCloudService(boolean b) {
    isCloudService = b;
  }

  public void setRequiresKeys(boolean b) {
    requiresKeys = b;
  }

  public boolean requiresKeys() {
    return requiresKeys;
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

  public void exclude(String groupId, String artifactId) {
    // get last dependency
    // dependencies
    if (lastDependency == null) {
      log.error("DEPENDENCY NOT DEFINED - CANNOT EXCLUDE");
    }
    lastDependency.add(new ServiceExclude(groupId, artifactId));
  }

  public void includeServiceInOneJar(Boolean b) {
    includeServiceInOneJar = b;
  }

  public boolean includeServiceInOneJar() {
    return includeServiceInOneJar;
  }

  public void addArtifact(String orgId, String classifierId) {
    lastDependency.add(new ServiceArtifact(orgId, classifierId));
  }

}
