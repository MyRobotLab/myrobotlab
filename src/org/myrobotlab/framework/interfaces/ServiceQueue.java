package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

// TODO - make 'local' & 'remote' messaging interfaces ?
// this one would be 'remote' - local would have addListener
// decompose Message Sender & Message Subscriber
public interface ServiceQueue {

  /**
   * put message in inbox, so it will be processed by this service
   * @param msg m
   */

  public void in(Message msg);

  public void out(Message msg);

  // TODO - put in seperate Invoking interface
  public Object invoke(String method);

  public Object invoke(String method, Object... params);

  // public boolean isLocal();

}
