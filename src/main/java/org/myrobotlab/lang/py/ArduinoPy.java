package org.myrobotlab.lang.py;

import java.util.Arrays;
import java.util.Map;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.InMoov2;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.interfaces.ServoControl;

public class ArduinoPy extends LangPyUtils {

  @Override
  public String toPython(ServiceInterface si) {
    
    // common stuff
    Arduino arduino = (Arduino) si;   
    StringBuilder content = new StringBuilder();
    String name = safeRefName(si);

    content.append("# Arduino Config : " + name + "\n");

    if (!arduino.isVirtual()) {
      content.append("# " + name + ".setVirtual(True)\n");
    } else {
      content.append(name + ".setVirtual(True)\n");
    }

    content.append("# we have the following ports : " + Arrays.toString(arduino.getPortNames().toArray()) + "\n");
    if (arduino.isConnected()) {
      content.append(String.format("%s.connect(\"%s\")\n", name, arduino.getPortName()));
    }

    // since the arduino is required a connection to attach its best if the
    // attach is done here after the connect
    // - Set<String> attached = arduino.getAttached();
    Map<String, DeviceMapping> devices = arduino.getDeviceList();
    content.append("# make sure the pins are set before attaching\n");
    for (String device : devices.keySet()) {
      Attachable s = devices.get(device).getDevice();
      if (ServoControl.class.isAssignableFrom(s.getClass())) {
        ServoControl sc = (ServoControl) s;
        content.append(safeRefName(sc.getName()) + ".setPin(\"" + sc.getPin() + "\")\n");
      }
    }
    for (String device : devices.keySet()) {
      Attachable s = devices.get(device).getDevice();
      if (ServoControl.class.isAssignableFrom(s.getClass())) {
        ServoControl sc = (ServoControl) s;
        content.append(String.format("%s.attach(\"%s\")\n", name, sc.getName()));
      }
    }

    return content.toString();
  }

}
