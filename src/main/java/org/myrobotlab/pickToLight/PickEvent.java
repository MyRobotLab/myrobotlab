package org.myrobotlab.pickToLight;

public class PickEvent {

  public String ipAddress;
  public String macAddress;
  public int i2CAddress;

  public PickEvent(Controller controller, Module m) {
    ipAddress = controller.getIpAddress();
    macAddress = controller.getMacAddress();
    i2CAddress = m.getI2CAddress();
  }

}
