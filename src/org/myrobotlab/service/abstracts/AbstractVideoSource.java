package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.VideoSink;
import org.myrobotlab.service.interfaces.VideoSource;

public abstract class AbstractVideoSource extends Service implements VideoSource {

  private static final long serialVersionUID = 1L;

  public AbstractVideoSource(String reservedKey) {
    super(reservedKey);
  }

  @Override
  public boolean attach(VideoSink vs){
    subscribe(vs.getName(), "publishDisplay");
    return true;
  }
  
  @Override
  public boolean detach(VideoSink vs) {
    unsubscribe(vs.getName(), "publishDisplay");
    return true;
  }

}
