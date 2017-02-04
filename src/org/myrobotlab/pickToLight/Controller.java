package org.myrobotlab.pickToLight;

import java.util.concurrent.ConcurrentHashMap;

public class Controller {

  private String name;
  private String version;
  private String ipAddress;
  private String macAddress;

  private ConcurrentHashMap<String, Module> modules = new ConcurrentHashMap<String, Module>();

  public Controller() {
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getMacAddress() {
    return macAddress;
  }

  public ConcurrentHashMap<String, Module> getModules() {
    return modules;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public void setMacAddress(String macAddress) {
    this.macAddress = macAddress;
  }

  public void setModules(ConcurrentHashMap<String, Module> modules) {
    this.modules = modules;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setVersion(String version) {
    this.version = version;
  }

}
