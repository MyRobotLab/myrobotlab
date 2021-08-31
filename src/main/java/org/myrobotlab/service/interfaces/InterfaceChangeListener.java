package org.myrobotlab.service.interfaces;

public interface InterfaceChangeListener {

  public void onInterfaceRegistered(String serviceName, String interfaceName);

  public void onInterfaceReleased(String serviceName, String interfaceName);
  
}
