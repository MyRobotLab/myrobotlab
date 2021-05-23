package org.myrobotlab.lang.py;

import java.util.Set;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.ProgramAB;

public class ProgramABPy extends LangPyUtils implements PythonGenerator {

  @Override
  public String toPython(ServiceInterface s) {
    return toPython((ProgramAB)s);
  }
  
  public String toPython(ProgramAB si) {
    // common stuff
    ProgramAB brain = (ProgramAB) si;   
    StringBuilder content = new StringBuilder();
    content.append(toDefaultPython(si));
    String safename = safeRefName(brain);

    content.append("# ProgramAB Config : " + si.getName() + "\n");
    content.append(String.format("%s = Runtime.start('%s', '%s')\n", safename, si.getName(), si.getSimpleName()));
    
    // lang 
    content.append(String.format("%s.setCurrentBotName('" + si.getCurrentBotName() + "')\n", safename));
    content.append(String.format("%s.setCurrentUserName('" + si.getCurrentUserName() + "')\n", safename));
    
    Set<String> attached = si.getAttached();
    for (String n : attached) {
      content.append(String.format("%s.attach('" + n + "')\n", safename));
    }

    return content.toString();
  }
}
