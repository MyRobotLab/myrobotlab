package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.VideoSink;
import org.myrobotlab.service.interfaces.VideoSource;

public abstract class AbstractVideoSource<C extends ServiceConfig> extends Service<C> implements VideoSource {

  private static final long serialVersionUID = 1L;

  public AbstractVideoSource(String n, String id) {
    super(n, id);
  }

  @Override
  public boolean attach(VideoSink vs) {
    subscribe(vs.getName(), "publishDisplay");
    return true;
  }

  @Override
  public boolean detach(VideoSink vs) {
    unsubscribe(vs.getName(), "publishDisplay");
    return true;
  }

}
