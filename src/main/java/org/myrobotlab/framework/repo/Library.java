package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.Comparator;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Library implements Serializable, Comparator<Library> {

  transient public final static Logger log = LoggerFactory.getLogger(Library.class);

  private static final long serialVersionUID = 1L;
  private String org;
  private String revision;
  private boolean installed = false;

  public Library() {
    this(null, null, true);
  }

  /**
   * constructor with key - of form [org/version]
   * @param key k
   */
  public Library(String key) {
    String[] split = key.split("/");
    if (split.length != 2) {
      String err = String.format("%s not a valid library key", key);
      log.error(err);
      throw new RuntimeException(err);
    }
    this.org = split[0];
    this.revision = split[1];
  }

  public Library(String organisation, String version) {
    this.org = organisation;
    this.revision = version;
  }

  public Library(String organisation, String version, boolean released) {
    this.org = organisation;
    this.revision = version;
  }

  @Override
  public int compare(Library o1, Library o2) {
    return o1.getOrg().compareTo(o2.getOrg());
  }

  public String getModule() {
    if (org == null) {
      return null;
    }
    int p = org.lastIndexOf(".");
    if (p == -1) {
      return org;
    } else {
      return org.substring(p + 1);
    }
  }

  public String getOrg() {
    return org;
  }

  public String getRevision() {
    return revision;
  }

  public boolean isInstalled() {
    return installed;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  @Override
  public String toString() {
    return String.format("%s %s %b", org, revision, installed);
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

  public String getKey() {
    return String.format("%s/%s", org, revision);
  }

}
