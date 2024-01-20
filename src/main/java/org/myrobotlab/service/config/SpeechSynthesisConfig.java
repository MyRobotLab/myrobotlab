package org.myrobotlab.service.config;

import java.util.Map;

import org.myrobotlab.framework.Plan;

public class SpeechSynthesisConfig extends ServiceConfig {

  public boolean mute = false;
  public boolean blocking = false;
  @Deprecated /* :(  ... this is already in listeners ! */
  public String[] speechRecognizers;
  public Map<String, String> substitutions;
  public String voice;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    // default names
    addDefaultPeerConfig(plan, name, "audioFile", "AudioFile");

    return plan;
  }

}
