package org.myrobotlab.process;

public interface UpdateListener {
  void onUpdateAvailable(String version);

  void onUpdateReady(String location, String type);
}
