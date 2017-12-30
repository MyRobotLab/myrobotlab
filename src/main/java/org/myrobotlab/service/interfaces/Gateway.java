/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.interfaces;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Status;
import org.myrobotlab.net.Connection;

public interface Gateway {

  public void addConnectionListener(String name);

  public void connect(String uri) throws URISyntaxException;

  /*
   * retrieves endpoint data for which this gateway is responsible
   * 
   */
  // DEPRECATE?
  public HashMap<URI, Connection> getClients();

  /*
   * important initial communication function related to discovery a broadcast
   * goes out and replies must include details of communication so that a viable
   * connection can be created
   */
  public List<Connection> getConnections(URI clientKey);

  public String getPrefix(URI protocolKey);

  /*
   * the publishing point
   */
  public Connection publishConnect(Connection keys);

  public void sendRemote(final String key, final Message msg) throws URISyntaxException;

  /*
   * will send a message to the mrl key'ed uri the expectation is the uri is
   * directly from the hosts registry in runtime therefore it has the following
   * format
   * 
   * mrl://[gatewayName]/proto://protohost:protoport/otherkeyinfo
   * 
   * e.g. a tcp connection throughh a RemoteAdapter instance named "remote"
   * would be
   * 
   * mrl://remote/tcp://somehost:6767
   * 
   * @param key - the url for the message
   */
  
  public void sendRemote(final URI key, final Message msg);

  // begin new interface methods -----------------------
  // FIXME ? - should publishConnection return a Connection object as with publishDisconnect ?
  public String publishConnect();
  public String publishDisconnect();
  public Status publishError();
}
