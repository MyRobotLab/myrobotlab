package org.myrobotlab.framework.repo;

import java.io.Serializable;

public class ServiceExclude implements Serializable {

  private static final long serialVersionUID = 1L;

  String org;
  String module;
  String name;
  String type;
  String ext;
  String matcher;
  String conf;
  String version;

  public ServiceExclude(String groupId, String artifactId) {
    this.org = groupId;
    this.module = artifactId;
  }

  public String getOrgId() {
    return org;
  }

  public String getArtifactId() {
    return module;
  }

  public String getVersion() {
    return version;
  }

  public String getType() {
    return type;
  }

  public String getExt() {
    return ext;
  }

}
