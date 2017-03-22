package org.myrobotlab.service.interfaces;

import org.myrobotlab.image.SerializableImage;

public interface VideoSource extends ServiceInterface {
  
  default public boolean attach(VideoSink vs){
    subscribe(vs.getName(), "publishDisplay");
    return true;
  }
  
  default public boolean detach(VideoSink vs) {
    unsubscribe(vs.getName(), "publishDisplay");
    return true;
  }
 
  public SerializableImage publishDisplay(SerializableImage img);

}
