package org.myrobotlab.service.interfaces;

import org.myrobotlab.image.SerializableImage;

public interface VideoSink extends ServiceInterface {

  // FIXME - waiting for Java 8 to have 'default' implementations
  
  default public boolean attach(VideoSource vs){
    subscribe(vs.getName(), "publishDisplay");
    return true;
  }
  

  default public boolean attachVideoSource(String videoSource){
    ServiceInterface si = org.myrobotlab.service.Runtime.getService(videoSource);
    if (si instanceof VideoSource) {
      return attach((VideoSource) si);
    }
  
    error("%s is not a VideoSource", videoSource);
    return false;
  }
  

  default public boolean detach(VideoSource vs){
    unsubscribe(vs.getName(), "publishDisplay");
    return true;
  }
  

  default public boolean detachVideoSource(String videoSource){
    ServiceInterface si = org.myrobotlab.service.Runtime.getService(videoSource);
    if (si instanceof VideoSource) {
      return detach((VideoSource) si);
    }

    error("%s is not a VideoSource", videoSource);
    return false;
  }
  

  public abstract void onDisplay(SerializableImage img);
}
