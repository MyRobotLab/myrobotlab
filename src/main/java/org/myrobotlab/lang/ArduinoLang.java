package org.myrobotlab.lang;

import java.util.Arrays;
import java.util.Map;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.interfaces.ServoControl;

public class ArduinoLang extends LangUtils {

  public String toPython(Arduino arduino) {
    StringBuilder sb = new StringBuilder();
    String name = safeRefName(arduino);

    sb.append("# Arduino Config : " + name + "\n");
    
    
    if (!arduino.isVirtual()) {
      sb.append("# " + name + ".setVirtual(True)\n");
    } else {
      sb.append(name + ".setVirtual(True)\n");
    }   
   
    sb.append("# we have the following ports : " +  Arrays.toString(arduino.getPortNames().toArray()) + "\n");    
    if (arduino.isConnected()) {
      sb.append(String.format("%s.connect(\"%s\")\n",name,  arduino.getPortName()));
    }
    
    // since the arduino is required a connection to attach its best if the attach is done here after the connect
    // - Set<String> attached = arduino.getAttached();
    Map<String, DeviceMapping> devices = arduino.getDeviceList();
    sb.append("# make sure the pins are set before attaching\n");
    for (String device : devices.keySet()) {
      Attachable s = devices.get(device).getDevice();
      if (ServoControl.class.isAssignableFrom(s.getClass())) {
        ServoControl sc = (ServoControl) s;
        sb.append(safeRefName(sc.getName()) + ".setPin(\""+sc.getPin()+"\")\n");
      }     
    }
    for (String device : devices.keySet()) {
      Attachable s = devices.get(device).getDevice();
      if (ServoControl.class.isAssignableFrom(s.getClass())) {
        ServoControl sc = (ServoControl) s;
        sb.append(String.format("%s.attach(\"%s\")\n",name,  sc.getName()));
      }     
    }
    
    
    return sb.toString();
  }

}
