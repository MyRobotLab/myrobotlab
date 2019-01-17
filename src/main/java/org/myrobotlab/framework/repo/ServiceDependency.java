package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ServiceDependency implements Serializable, Comparator<ServiceDependency> {

  transient public final static Logger log = LoggerFactory.getLogger(ServiceDependency.class);

  private static final long serialVersionUID = 1L;
  private String groupId;
  private String artifactId;
  private String version;
  private String ext; // ext

  private boolean installed = false;

  private List<ServiceExclude> excludes = new ArrayList<ServiceExclude>();
  private List<ServiceArtifact> artifacts = new ArrayList<ServiceArtifact>();

  public ServiceDependency() {
    this(null, null, null);
  }

  /**
   * constructor with key - of form [org/version]
   * 
   * @param key
   *          k
   */
  public ServiceDependency(String key) {
    String[] split = key.split("/");

    if (split.length == 2) {
      this.groupId = split[0];
      this.version = split[1];
    } else if (split.length == 3) {
      this.groupId = split[0];
      this.artifactId = split[1];
      this.version = split[2];
    } else if (split.length == 4) {
      this.groupId = split[0];
      this.artifactId = split[1];
      this.version = split[2];
      if (!split[3].equals("null")) {
        this.ext = split[3];
      }
    } else {
      String err = String.format("%s not a valid library key", key);
      log.error(err);
      throw new RuntimeException(err);
    }
  }

  public ServiceDependency(String organisation, String artifactId, String version) {
    this.groupId = organisation;
    this.artifactId = artifactId;
    this.version = version;
  }

  public ServiceDependency(String groubId, String artifactId, String version, String ext) {
    this.groupId = groubId;
    this.artifactId = artifactId;
    this.version = version;
    this.ext = ext;
  }

  @Override
  public int compare(ServiceDependency o1, ServiceDependency o2) {
    return o1.getKey().compareTo(o2.getKey());
  }

  public String getOrgId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getExt() {
    return ext;
  }

  public boolean isInstalled() {
    return installed;
  }

  public void setRevision(String revision) {
    this.version = revision;
  }

  @Override
  public String toString() {
    // return String.format("%s %b", getKey(), installed);
    return getKey();
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

  public String getKey() {
    return String.format("%s/%s/%s/%s", groupId, artifactId, version, ext);
  }

  public void add(ServiceExclude serviceExclude) {
    excludes.add(serviceExclude);
  }

  public List<ServiceExclude> getExcludes() {
    return excludes;
  }

  public void add(ServiceArtifact serviceArtifact) {
    artifacts.add(serviceArtifact);
  }

}
