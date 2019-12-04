package org.myrobotlab.lang;

import org.myrobotlab.service.WebGui;

public class WebGuiLang extends LangUtils {

  public String toPython(WebGui s) {
    StringBuilder sb = new StringBuilder();
    String name = safeRefName(s);

    sb.append("# WebGui Config : " + name + "\n");
    sb.append(name + ".autoStartBrowser(" + toPython(s.getAutoStartBrowser()) + ")\n");
    sb.append(name + ".setPort(" + s.getPort() + ")\n");
    sb.append(name + ".setAddress(" + escape(s.getAddress()) + ")\n");
    
    return sb.toString();
  }

}
