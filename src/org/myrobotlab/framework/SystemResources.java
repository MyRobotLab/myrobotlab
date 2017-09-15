package org.myrobotlab.framework;

import java.io.Serializable;

public class SystemResources implements Serializable {

  private static final long serialVersionUID = 1L;
  // changing values
  long totalPhysicalMemory;
  long totalMemory;
  long freeMemory;
  long maxMemory;

  public SystemResources() {

    try {
      com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
      totalPhysicalMemory = os.getTotalPhysicalMemorySize() / 1048576;

    } catch (Exception e) {
    }

    freeMemory = Runtime.getRuntime().freeMemory() / 1048576;
    totalMemory = Runtime.getRuntime().totalMemory() / 1048576;
    maxMemory = Runtime.getRuntime().maxMemory() / 1048576;
  }

  public long getTotalPhysicalMemory() {
    return totalPhysicalMemory;
  }

  public long getTotalMemory() {
    return totalMemory;
  }

  public long getFreeMemory() {
    return freeMemory;
  }

  public long getMaxMemory() {    
    return maxMemory;
  }

}
