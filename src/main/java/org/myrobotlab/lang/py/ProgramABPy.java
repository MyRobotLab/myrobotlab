package org.myrobotlab.lang.py;

import java.util.Set;

import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.util.FormatUtil;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.ProgramAB;

public class ProgramABPy extends LangPyUtils implements PythonGenerator {

  @Override
  public String toPython(ServiceInterface s) {
    return toPython((ProgramAB) s);
  }

  public String toPython(ProgramAB si) {
    // common stuff
    ProgramAB brain = si;
    StringBuilder content = new StringBuilder();
    String safename = safeRefName(brain);

    // lang
    content.append(String.format(FormatUtil.asFormat("  " + "%s.setCurrentBotName('" + si.getCurrentBotName() + "')\n", ConversionCategory.GENERAL), safename));
    content.append(String.format(FormatUtil.asFormat("  " + "%s.setCurrentUserName('" + si.getCurrentUserName() + "')\n", ConversionCategory.GENERAL), safename));

    brain.getAttached();

    Set<String> attached = si.getAttached("publishText");
    for (String n : attached) {
      if (!n.contains("@")) {
        content.append(String.format(FormatUtil.asFormat("  " + "%s.attachTextListener('" + n + "')\n", ConversionCategory.GENERAL), safename));
      }
    }

    return content.toString();
  }
}
