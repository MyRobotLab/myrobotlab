/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
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
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.NameProvider;

public interface Gateway extends NameProvider {

  // public void addConnectionListener(String name);

  public void connect(String uri) throws Exception;

  public List<String> getClientIds();
  
  public Map<String, Map<String, Object>> getClients();

  // public List<Connection> getConnections(URI clientKey);

  // public String getPrefix(URI protocolKey);

  // public Connection publishConnect(Connection keys);

  public void sendRemote(final String key, final Message msg) throws URISyntaxException;

  public void sendRemote(final URI key, final Message msg);
  
  public Object sendBlockingRemote(Message msg, Integer timeout) throws Exception;

  // public String publishConnect();

  // public String publishDisconnect();

  // public Status publishError();
}
