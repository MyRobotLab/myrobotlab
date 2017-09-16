package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.interfaces.VideoSink;
import org.myrobotlab.service.interfaces.VideoSource;

public abstract class AbstractVideoSink extends Service implements VideoSink {

  private static final long serialVersionUID = 1L;


  public AbstractVideoSink(String reservedKey) {
    super(reservedKey);
  }

  public boolean attach(VideoSource vs){
    subscribe(vs.getName(), "publishDisplay");
    return true;
  }
  
  @Override
  public boolean attachVideoSource(String videoSource){
    ServiceInterface si = org.myrobotlab.service.Runtime.getService(videoSource);
    if (si instanceof VideoSource) {
      return attach((VideoSource) si);
    }
  
    error("%s is not a VideoSource", videoSource);
    return false;
  }
  

  public boolean detach(VideoSource vs){
    unsubscribe(vs.getName(), "publishDisplay");
    return true;
  }
  
  @Override
  public boolean detachVideoSource(String videoSource){
    ServiceInterface si = org.myrobotlab.service.Runtime.getService(videoSource);
    if (si instanceof VideoSource) {
      return detach((VideoSource) si);
    }

    error("%s is not a VideoSource", videoSource);
    return false;
  }
  

  abstract public void onDisplay(SerializableImage img);

}
