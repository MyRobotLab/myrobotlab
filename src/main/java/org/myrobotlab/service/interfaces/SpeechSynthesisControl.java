package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.WordFilter;

public interface SpeechSynthesisControl extends NameProvider {

  public void speak(String text);

  public void setVoice(String name);
  
  public void setVolume(double volume);
  
  public void setMute(boolean mute);
  
  public void replaceWord(String word, String substitute);
  
  public String publishSpeak(String text);
  
  public String publishSetVoice(String name);

  public Double publishSetVolume(Double volume);
  
  public Boolean publishSetMute(Boolean mute);

  public WordFilter publishReplaceWord(String word, String substitute);
  
}
