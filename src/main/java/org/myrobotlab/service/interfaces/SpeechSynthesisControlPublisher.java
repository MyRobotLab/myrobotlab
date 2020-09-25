package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.WordFilter;

public interface SpeechSynthesisControlPublisher extends NameProvider {

  public String publishSpeak(String text);

  public String publishSetVoice(String name);

  public Double publishSetVolume(Double volume);

  public Boolean publishSetMute(Boolean mute);

  public WordFilter publishReplaceWord(String word, String substitute);

}
