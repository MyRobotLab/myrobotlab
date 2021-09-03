package org.myrobotlab.lang.py;

import java.util.Set;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.ProgramAB;

public class ProgramABPy extends LangPyUtils implements PythonGenerator {

  @Override
  public String toPython(ServiceInterface s) {
    return toPython((ProgramAB) s);
  }

  public String toPython(ProgramAB si) {
    // common stuff
    ProgramAB brain = (ProgramAB) si;
    StringBuilder content = new StringBuilder();
    String safename = safeRefName(brain);

    // lang
    content.append(String.format("  " + "%s.setCurrentBotName('" + si.getCurrentBotName() + "')\n", safename));
    content.append(String.format("  " + "%s.setCurrentUserName('" + si.getCurrentUserName() + "')\n", safename));

    brain.getAttached();

    Set<String> attached = si.getAttached("publishText");
    for (String n : attached) {
      if (!n.contains("@")) {
        content.append(String.format("  " + "%s.attachTextListener('" + n + "')\n", safename));
      }
    }

    return content.toString();
  }
}
