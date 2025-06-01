package org.myrobotlab.framework;

public class DescribeQuery {

  /**
   * id of the foreign process
   */
  public String id;
  /**
   * connection uuid
   */
  public String uuid;

  /**
   * platform of the foreign process
   */
  public Platform platform;

  public DescribeQuery(String id, String uuid) {
    this.id = id;
    this.uuid = uuid;
    this.platform = Platform.getLocalInstance();
  }

  @Override
  public String toString() {
    return String.format("%s - %s - %s", uuid, id, platform);
  }
}
