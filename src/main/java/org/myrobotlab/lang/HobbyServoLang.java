package org.myrobotlab.lang;

import org.myrobotlab.service.HobbyServo;

public class HobbyServoLang extends LangUtils {

  public String toPython(HobbyServo s) {
    StringBuilder sb = new StringBuilder();
    String name = safeRefName(s);

    sb.append("# HobbyServo Config : " + name + "\n");
    // why ?? sb.append(name + ".detach()\n");
    // sb.append(name + ".detach()\n");
    sb.append("# sets initial position of servo before moving\n");
    sb.append("# in theory this is the position of the servo when this file was created\n");
    sb.append(name + String.format(".setPosition(%.1f)\n", s.getPos()));
    sb.append(name + ".setMinMax(" + s.getMin() + "," + s.getMax() + ")\n");
    sb.append(name + ".setVelocity(" + s.getSpeed() + ")\n");
    sb.append(name + ".setRest(" + s.getRest() + ")\n");
    if (s.getPin() != null) {
      sb.append(name + ".setPin(" + s.getPin() + ")\n");
    } else {
      sb.append("# " + name + ".setPin(" + s.getPin() + ")\n");
    }

    s.map(s.getMin(), s.getMax(), s.getMinOutput(), s.getMaxOutput());
    // save the servo map
    sb.append(name + ".map(" + s.getMin() + "," + s.getMax() + "," + s.getMinOutput() + "," + s.getMaxOutput() + ")\n");
    // if there's a controller reattach it at rest
    // FIXME - there is the initial position vs rest - they potentially are very different
    /*
    if (s.getControllerName() != null) {
      String controller = s.getControllerName();
      sb.append(name + ".attach(\"" + controller + "\"," + s.getPin() + "," + s.getRest() + ")\n");
    }*/
    
    if (s.getControllerName() != null) {
      String controller = s.getControllerName();
      sb.append(name + ".attach(\"" + controller + "\")\n");
    }
    
    if (s.getAutoDisable()) {
      sb.append(name + ".setAutoDisable(True)\n");
    }
    return sb.toString();
  }

}
