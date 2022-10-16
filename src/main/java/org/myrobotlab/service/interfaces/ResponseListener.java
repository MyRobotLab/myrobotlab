package org.myrobotlab.service.interfaces;

import org.myrobotlab.programab.Response;

/**
 * An utterance Listener. It can be attached to by an Response publisher.
 *
 */
public interface ResponseListener {

  public String getName();

  public void onResponse(Response response) throws Exception;

}
