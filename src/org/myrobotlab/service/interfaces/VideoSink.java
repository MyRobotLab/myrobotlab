package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.SerializableImage;

public interface VideoSink extends ServiceInterface {
  
  public boolean attach(VideoSource vs);
  
  public boolean attachVideoSource(String videoSource);

  public boolean detach(VideoSource vs);

  public boolean detachVideoSource(String videoSource);

  public abstract void onDisplay(SerializableImage img);
}
