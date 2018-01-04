package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.Comparator;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Library implements Serializable, Comparator<Library> {

  transient public final static Logger log = LoggerFactory.getLogger(Library.class);

  private static final long serialVersionUID = 1L;
  private String groupId;
  private String artifactId;
  private String version;
  
  private boolean installed = false;

  public Library() {
    this(null, null, null, true);
  }

  /**
   * constructor with key - of form [org/version]
   * @param key k
   */
  public Library(String key) {
    String[] split = key.split("/");
    
    if (split.length == 2){
      this.groupId = split[0];
      this.version = split[1];
    } else if (split.length == 3) {
      this.groupId = split[0];
      this.artifactId = split[1];
      this.version = split[2];
    } else {
      String err = String.format("%s not a valid library key", key);
      log.error(err);
      throw new RuntimeException(err);
    }
  }
/*
  public Library(String organisation, String version) {
    this.groupId = organisation;
    this.version = version;
  }
*/
  public Library(String organisation, String artifactId, String version, boolean released) {
    this.groupId = organisation;
    this.artifactId = artifactId;
    this.version = version;
  }

  @Override
  public int compare(Library o1, Library o2) {
    return o1.getOrg().compareTo(o2.getOrg());
  }

  public String getModule() {
    if (groupId == null) {
      return null;
    }
    int p = groupId.lastIndexOf(".");
    if (p == -1) {
      return groupId;
    } else {
      return groupId.substring(p + 1);
    }
  }

  public String getOrg() {
    return groupId;
  }
  
  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public boolean isInstalled() {
    return installed;
  }

  public void setRevision(String revision) {
    this.version = revision;
  }

  @Override
  public String toString() {
    return String.format("%s %s %s %b", groupId, artifactId, version, installed);
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

  public String getKey() {
    return String.format("%s/%s", groupId, version);
  }

}
