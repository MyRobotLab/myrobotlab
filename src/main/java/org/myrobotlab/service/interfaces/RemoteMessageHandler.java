package org.myrobotlab.service.interfaces;

public interface RemoteMessageHandler {
  public void onRemoteMessage(String uuid, String data);
}
