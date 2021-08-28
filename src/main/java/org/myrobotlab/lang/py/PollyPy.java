package org.myrobotlab.lang.py;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.Polly;

public class PollyPy extends LangPyUtils implements PythonGenerator {

  @Override
  public String toPython(ServiceInterface si) {

    // common stuff
    Polly polly = (Polly) si;
    StringBuilder content = new StringBuilder();
    String name = safeRefName(polly);

    content.append("  " + "# Polly config for " + name + "\n");

    // Too many
    // List<Voice> voices = polly.getVoices();
    //
    // for (Voice voice : voices) {
    // content.append("# " + name + ".setVoice(\"" + voice.getName() + "\")" + "
    // # " + voice.getLanguage() + "\n");
    // }

    if (polly.getVoice() != null) {
      content.append("  " + name + ".setVoice(\"" + polly.getVoice().getName() + "\")\n");
    }

    content.append("  " + name + ".setMute(" + toPython(polly.isMute()) + ")\n");

    return content.toString();
  }

}
