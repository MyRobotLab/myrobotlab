package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface SpeechSynthesisControl extends NameProvider {

  public String speak(String text);

  public String setVoice(String name);

  public double setVolume(double volume);

  public boolean setMute(boolean mute);

  public void replaceWord(String word, String substitute);

}
