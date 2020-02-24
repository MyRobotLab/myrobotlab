package org.myrobotlab.lang;

import org.myrobotlab.service.InMoov2;

public class InMoov2Lang extends LangUtils {

  public String toPython(InMoov2 inmoov2) {
    StringBuilder sb = new StringBuilder();
    String name = safeRefName(inmoov2);

    sb.append("# InMoov2 Config : " + name + "\n");

    if (!inmoov2.isVirtual()) {
      sb.append("# " + name + ".setVirtual(True)\n");
    } else {
      sb.append(name + ".setVirtual(True)\n");
    }
    
    sb.append(name + ".setMute("+ toPython(inmoov2.isMute())+")\n");
    
    
    if (inmoov2.isMute())

    sb.append(name + ".setLanguage(" + inmoov2.getLanguage() + ")\n");

    sb.append("# start groups of sub services\n");

    if (inmoov2.getHead() != null) {
      sb.append(name + ".startHead()\n");
    }

    if (inmoov2.getLeftHand() != null) {
      sb.append(name + ".startLeftHand()\n");
    }

    if (inmoov2.getRightHand() != null) {
      sb.append(name + ".getRightHand()\n");
    }

    if (inmoov2.getLeftArm() != null) {
      sb.append(name + ".startLeftArm()\n");
    }
    
    if (inmoov2.getSimulator() != null) {
      sb.append(name + ".startSimulator()\n");
    }

    if (inmoov2.getRightArm() != null) {
      sb.append(name + ".getRightArm()\n");
    }

    if (inmoov2.getTorso() != null) {
      sb.append(name + ".startTorso()\n");
    }

    return sb.toString();
  }

}
