package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;

public class ServoMixer extends Service {

  private static final long serialVersionUID = 1L;

  public ServoMixer(String reservedKey) {
    super(reservedKey);
  }

  public List<ServoControl> listAllServos() {
    ArrayList<ServoControl> servos = new ArrayList<ServoControl>();
    // TODO: get a list of all servos
    for (ServiceInterface service : Runtime.getServices()) {
      if (service instanceof ServoControl) {
        servos.add((ServoControl)service);
      }
    }
    return servos;
  }
  
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(ServoMixer.class.getCanonicalName());
    meta.addDescription("ServoMixer - most just a swing gui that allows for simple movements of all servos in one gui panel.");
    return meta;
  }
  
  public static void main(String[] args) {
    Runtime.start("gui", "SwingGui");
    Runtime.start("servo1", "Servo");
    Runtime.start("servo2", "Servo");
    Runtime.start("servo3", "Servo");
    ServoMixer mixer = (ServoMixer)Runtime.start("servomixer", "ServoMixer");
  }
  
}