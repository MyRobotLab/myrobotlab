package org.myrobotlab.lang;

import java.text.DecimalFormat;

import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.Servo;


public class ServoLang extends LangUtils {

  transient static DecimalFormat f = new DecimalFormat("#.##");

  public String toPython(Servo s) {
    StringBuilder sb = new StringBuilder();
    String name = safeRefName(s);

    sb.append("# Servo Config : " + name + "\n");
    // why ?? sb.append(name + ".detach()\n");
    // sb.append(name + ".detach()\n");
    sb.append("# sets initial position of servo before moving\n");
    sb.append("# in theory this is the position of the servo when this file was created\n");
    sb.append(name + String.format(".setPosition(%s)\n", f.format(s.getCurrentInputPos())));
    sb.append(name + ".map(" + s.getMapper().getMinX() + "," + s.getMapper().getMaxX() + "," +
                               s.getMapper().getMinY() + "," + s.getMapper().getMaxY() + ")\n");
    sb.append(name + ".setInverted(" + toPython(s.isInverted()) + ")\n");
    sb.append(name + ".setSpeed(" + toPython(s.getSpeed()) + ")\n");
    sb.append(name + ".setRest(" + s.getRest() + ")\n");
    if (s.getPin() != null) {
      sb.append(name + ".setPin(" + s.getPin() + ")\n");
    } else {
      sb.append("# " + name + ".setPin(" + s.getPin() + ")\n");
    }

    // if there's a controller reattach it at rest
    // FIXME - there is the initial position vs rest - they potentially are very different
    /*
    if (s.getControllerName() != null) {
      String controller = s.getControllerName();
      sb.append(name + ".attach(\"" + controller + "\"," + s.getPin() + "," + s.getRest() + ")\n");
    }*/
    /*  the dependencies on the controller are higher - so let it attach to this servo 
    if (s.getControllerName() != null) {
      String controller = s.getControllerName();
      sb.append(name + ".attach(\"" + controller + "\")\n");
    }
    */
    if (s.getAutoDisable()) {
      sb.append(name + ".setAutoDisable(True)\n");
    } else {
      sb.append(name + ".setAutoDisable(False)\n");      
    }
    return sb.toString();
  }

}
