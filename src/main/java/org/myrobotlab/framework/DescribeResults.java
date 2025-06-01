package org.myrobotlab.framework;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.codec.CodecUtils;

public class DescribeResults {
  protected String id;
  protected String uuid;
  protected DescribeQuery request;
  protected Platform platform;
  protected Status status;
  protected List<Registration> registrations;

  public void setId(String id) {
    this.id = id;
  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return CodecUtils.toJson(this);
  }

  public void addRegistration(Registration registration) {
    if (registrations == null) {
      registrations = new ArrayList<>();
    }
    registrations.add(registration);
  }

  public List<Registration> getReservations() {
    return registrations;
  }

}
