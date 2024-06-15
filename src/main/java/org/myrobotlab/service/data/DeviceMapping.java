package org.myrobotlab.service.data;

import java.io.Serializable;

import org.myrobotlab.framework.interfaces.Attachable;

public class DeviceMapping implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * the unique integer id for this device - this is how MrlComm identifies the
   * device over the mrl comm protocol
   */
  private Integer id;

  /**
   * attached device
   */
  transient Attachable device;

  public DeviceMapping(int id, Attachable device) {
    this.id = id;
    this.device = device;
  }

  public String getName() {
    return device.getName();
  }

  public Integer getId() {
    return id;
  }

  public Attachable getDevice() {
    return device;
  }

  @Override
  public String toString() {
    return String.format("id:%d name:%s", id, device.getName());
  }
}
