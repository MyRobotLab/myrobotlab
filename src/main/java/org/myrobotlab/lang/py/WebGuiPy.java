package org.myrobotlab.lang.py;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.WebGui;

public class WebGuiPy extends LangPyUtils implements PythonGenerator {
  @Override
  public String toPython(ServiceInterface si) {
    // common stuff
    WebGui webgui = (WebGui) si;
    StringBuilder content = new StringBuilder();
    content.append(toDefaultPython(si));
    String name = safeRefName(si);

    content.append("# WebGui Config : " + name + "\n");
    content.append(name + ".autoStartBrowser(" + toPython(webgui.getAutoStartBrowser()) + ")\n");
    content.append(name + ".setPort(" + webgui.getPort() + ")\n");
    content.append(name + ".setAddress(" + escape(webgui.getAddress()) + ")\n");

    return content.toString();
  }

}
