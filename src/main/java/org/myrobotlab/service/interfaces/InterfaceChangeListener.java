package org.myrobotlab.service.interfaces;

public interface InterfaceChangeListener {

  public void onAddInterface(String serviceName, String interfaceName);

  public void onRemoveInterface(String serviceName, String interfaceName);
  
}
