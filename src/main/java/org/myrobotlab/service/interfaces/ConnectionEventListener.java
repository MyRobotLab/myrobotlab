package org.myrobotlab.service.interfaces;

import okhttp3.Response;
import okhttp3.WebSocket;

/**
 * Start with interface exported from okhttpe. Next abstract it
 * and use only pojos for parameters. 
 * 
 * TODO - use for other byte stream event handlers like serial port listeners
 * 
 * @author GroG
 *
 */
public interface ConnectionEventListener {

  public void onOpen(WebSocket webSocket, Response response);
  
  public void onClosing(WebSocket webSocket, int code, String reason);

  public void onFailure(WebSocket webSocket, Throwable t, Response response);

}
