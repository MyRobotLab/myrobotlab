package org.myrobotlab.lang;

import java.util.List;

import org.myrobotlab.service.MarySpeech;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;

public class MarySpeechLang extends LangUtils {

  public String toPython(MarySpeech mary) {
    StringBuilder sb = new StringBuilder();
    String name = safeRefName(mary);

    sb.append("# MarySpeech Config : " + name + "\n");

    /*
    if (!mary.isVirtual()) {
      sb.append("# " + name + ".setVirtual(True)\n");
    } else {
      sb.append(name + ".setVirtual(True)\n");
    }*/
    
    List<Voice> voices = mary.getVoices();
    
    for (Voice voice : voices) {
      sb.append("# " + name + ".setVoice(\"" + voice.getName() + "\")" + " # " + voice.getLanguage() + "\n");
    }
    
    if (mary.getVoice() != null) {
      sb.append(name + ".setVoice(\"" + mary.getVoice().getName() + "\")\n");
    }
    
    sb.append(name + ".setMute("+ toPython(mary.isMute())+")\n");
    

    return sb.toString();
  }

}
