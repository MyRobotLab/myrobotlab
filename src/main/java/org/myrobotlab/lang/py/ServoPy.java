package org.myrobotlab.lang.py;

import java.text.DecimalFormat;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.Servo;

public class ServoPy extends LangPyUtils implements PythonGenerator {

  transient static DecimalFormat f = new DecimalFormat("#.##");

  @Override
  public String toPython(ServiceInterface si) {
    // common stuff
    Servo servo = (Servo) si;
    StringBuilder content = new StringBuilder();
    String name = safeRefName(si);

    content.append("# Servo Config : " + name + "\n");
    // why ?? sb.append(name + ".detach()\n");
    // sb.append(name + ".detach()\n");
    content.append("# sets initial position of servo before moving\n");
    content.append("# in theory this is the position of the servo when this file was created\n");
    content.append(name + String.format(".setPosition(%s)\n", f.format(servo.getCurrentInputPos())));
    content
        .append(name + ".map(" + servo.getMapper().getMinX() + "," + servo.getMapper().getMaxX() + "," + servo.getMapper().getMinY() + "," + servo.getMapper().getMaxY() + ")\n");
    // TODO: add mapper isClipped()
    content.append(name + ".setInverted(" + toPython(servo.isInverted()) + ")\n");
    content.append(name + ".setSpeed(" + toPython(servo.getSpeed()) + ")\n");
    content.append(name + ".setRest(" + servo.getRest() + ")\n");
    if (servo.getPin() != null) {
      content.append(name + ".setPin(" + servo.getPin() + ")\n");
    } else {
      content.append("# " + name + ".setPin(" + servo.getPin() + ")\n");
    }

    // if there's a controller reattach it at rest
    // FIXME - there is the initial position vs rest - they potentially are very
    // different
    /*
     * if (s.getControllerName() != null) { String controller =
     * s.getControllerName(); sb.append(name + ".attach(\"" + controller + "\","
     * + s.getPin() + "," + s.getRest() + ")\n"); }
     */
    /*
     * the dependencies on the controller are higher - so let it attach to this
     * servo if (s.getControllerName() != null) { String controller =
     * s.getControllerName(); sb.append(name + ".attach(\"" + controller +
     * "\")\n"); }
     */
    if (servo.isAutoDisable()) {
      content.append(name + ".setAutoDisable(True)\n");
    } else {
      content.append(name + ".setAutoDisable(False)\n");
    }
    return content.toString();
  }

}
