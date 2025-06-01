package org.myrobotlab.framework.repo;

public class ServiceArtifact {

  String orgId;
  String classifierId;
  String type;

  public ServiceArtifact(String orgId, String classifierId) {
    this(orgId, classifierId, null);
  }

  public ServiceArtifact(String orgId, String classifierId, String type) {
    this.orgId = orgId;
    this.classifierId = classifierId;
    this.type = type;
  }

}
