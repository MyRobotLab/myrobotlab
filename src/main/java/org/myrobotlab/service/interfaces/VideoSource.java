package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.SerializableImage;

public interface VideoSource extends ServiceInterface {
  
  public boolean attach(VideoSink vs);
  
  public boolean detach(VideoSink vs);
  
  public SerializableImage publishDisplay(SerializableImage img);

}
