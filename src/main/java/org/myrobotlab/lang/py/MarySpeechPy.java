package org.myrobotlab.lang.py;

import java.util.List;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.MarySpeech;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;

public class MarySpeechPy extends LangPyUtils implements PythonGenerator {

  // FIXME - super inheritance of AbstractSpeechSynthesis

  @Override
  public String toPython(ServiceInterface si) {

    // common stuff
    MarySpeech mary = (MarySpeech) si;
    StringBuilder content = new StringBuilder();
    String name = safeRefName(mary);

    content.append("  " + "# MarySpeech Config : " + name + "\n");

    /*
     * if (!mary.isVirtual()) { sb.append("# " + name + ".setVirtual(True)\n");
     * } else { sb.append(name + ".setVirtual(True)\n"); }
     */

    List<Voice> voices = mary.getVoices();

    for (Voice voice : voices) {
      content.append("  " + "# " + name + ".setVoice(\"" + voice.getName() + "\")" + " # " + voice.getLanguage() + "\n");
    }

    if (mary.getVoice() != null) {
      content.append("  " + name + ".setVoice(\"" + mary.getVoice().getName() + "\")\n");
    }

    content.append("  " + name + ".setMute(" + toPython(mary.isMute()) + ")\n");

    return content.toString();
  }

}
