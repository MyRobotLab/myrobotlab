package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

// TODO - make 'local' & 'remote' messaging interfaces ?
// this one would be 'remote' - local would have addListener
// decompose Message Sender & Message Subscriber
public interface ServiceQueue {

  /**
   * 
   * @param msg
   *          put message in inbox, so it will be processed by this service
   * 
   * 
   */

  public void in(Message msg);

  public void out(Message msg);

  // public boolean isLocal();

}
