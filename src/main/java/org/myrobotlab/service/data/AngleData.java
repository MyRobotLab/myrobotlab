package org.myrobotlab.service.data;

/**
 * 
 * Pojo to publish angles from IKJointAnglePublishers
 *
 */
public class AngleData {

  protected final String name;
  protected final Double angle;

  public AngleData(String name, Double angle) {
    this.name = name;
    this.angle = angle;
  }

  public String getName() {
    return name;
  }

  public Double getAngle() {
    return angle;
  }

}
