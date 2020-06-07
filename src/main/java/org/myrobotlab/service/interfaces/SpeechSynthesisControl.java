package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.WordFilter;

public interface SpeechSynthesisControl extends NameProvider {

  public String speak(String text);

  public String setVoice(String name);
  
  public double setVolume(double volume);
  
  public boolean setMute(boolean mute);
  
  public void replaceWord(String word, String substitute);
  
  public String publishSpeak(String text);
  
  public String publishSetVoice(String name);

  public Double publishSetVolume(Double volume);
  
  public Boolean publishSetMute(Boolean mute);

  public WordFilter publishReplaceWord(String word, String substitute);
  
}
