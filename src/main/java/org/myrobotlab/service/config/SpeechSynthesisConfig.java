package org.myrobotlab.service.config;

import java.util.Map;

import org.myrobotlab.framework.Plan;

public class SpeechSynthesisConfig extends ServiceConfig {

  public boolean mute = false;
  public boolean blocking = false;
  public String[] speechRecognizers;
  public Map<String, String> substitutions;
  public String voice;
  
  // peer names
  public String audioFile;
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
        
    // default names
    audioFile = name + ".audioFile";    
    addPeer(plan, name, "audioFile", audioFile, "AudioFile", "AudioFile");

    return plan;
  }


}
