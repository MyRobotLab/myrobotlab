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
    sb.append(name + String.format(".setPosition(%s)\n", f.format(s.getPos())));
    sb.append(name + ".setMinMax(" + s.getMin() + "," + s.getMax() + ")\n");
    sb.append(name + ".setInverted(" + toPython(s.isInverted()) + ")\n");
    sb.append(name + ".setSpeed(" + toPython(s.getSpeed()) + ")\n");
    sb.append(name + ".setRest(" + s.getRest() + ")\n");
    if (s.getPin() != null) {
      sb.append(name + ".setPin(" + s.getPin() + ")\n");
    } else {
      sb.append("# " + name + ".setPin(" + s.getPin() + ")\n");
    }

    Mapper mapper = s.getMapper();
    
    
    // save the servo map
    sb.append(name + ".map(" + mapper.getMinX() + "," + mapper.getMaxX() + "," + mapper.getMinY() + "," + mapper.getMaxY() + ")\n");
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
    }
    return sb.toString();
  }

}
