package org.myrobotlab.lang.py;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.InMoov2;

public class InMoov2Py extends LangPyUtils {


  public String toPython(ServiceInterface si) {
    
    // common stuff
    InMoov2 inmoov2 = (InMoov2) si;   
    StringBuilder content = new StringBuilder();
    String name = safeRefName(si);

    content.append("  " +"# InMoov2 Config : " + name + "\n");

    if (!inmoov2.isVirtual()) {
      content.append("  " + "# " + name + ".setVirtual(True)\n");
    } else {
      content.append("  " + name + ".setVirtual(True)\n");
    }

    content.append("  " + name + ".setMute(" + toPython(inmoov2.isMute()) + ")\n");

    if (inmoov2.isMute())

      content.append("  " + name + ".setLanguage(" + inmoov2.getLanguage() + ")\n");

    content.append("  " + "# start groups of sub services\n");

    // FIXME - WRONG WRONG WRONG - NO DIRECT REFERENCE !!!!
    // IF THERE IS COMPLEX SETUP NECESSARY - TEST BY serviceType/peer/state
    if (inmoov2.getHead() != null) {
      content.append("  " + name + ".startHead()\n");
    }

    if (inmoov2.getLeftHand() != null) {
      content.append("  " + name + ".startLeftHand()\n");
    }

    if (inmoov2.getRightHand() != null) {
      content.append("  " + name + ".getRightHand()\n");
    }

    if (inmoov2.getLeftArm() != null) {
      content.append("  " + name + ".startLeftArm()\n");
    }

    if (inmoov2.getSimulator() != null) {
      content.append("  " + name + ".startSimulator()\n");
    }

    if (inmoov2.getRightArm() != null) {
      content.append("  " + name + ".getRightArm()\n");
    }

    if (inmoov2.getTorso() != null) {
      content.append("  " + name + ".startTorso()\n");
    }

    return content.toString();
  }

}
