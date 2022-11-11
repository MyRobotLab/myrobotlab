package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.Utterance;

/**
 * An utterance Listener. It can be attached to by an Utterance publisher.
 *
 */
public interface UtteranceListener {

  public String getName();

  public void onUtterance(Utterance utterance) throws Exception;

}
