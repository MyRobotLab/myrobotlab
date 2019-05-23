package org.myrobotlab.lang;

import java.util.Arrays;

import org.myrobotlab.service.Arduino;

public class ArduinoLang extends LangUtils {

  public String toPython(Arduino s) {
    StringBuilder sb = new StringBuilder();
    String name = safeRefName(s);

    sb.append("# Arduino Config : " + name + "\n");
   
    sb.append("# we have the followin ports : " +  Arrays.toString(s.getPortNames().toArray()) + "\n");    
    if (s.isConnected()) {
      sb.append(String.format("%s.connect(\"%s\")",name,  s.getPortName()));
    }
    return sb.toString();
  }

}
