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

import java.util.List;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.DescribeQuery;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.Runtime;

public interface Gateway extends NameProvider {

  /**
   * A constant used to determine whether a call to {@link Runtime#describe(String, DescribeQuery)}
   * should fill the UUID field or not.
   */
  String FILL_UUID_MAGIC_VAL = "fill-uuid";

  public void connect(String uri) throws Exception; // <-- FIXME invalid I
                                                    // assume ?

  public List<String> getClientIds();

  // FIXME - change to getConnections !!...
  // TODO getConnection() would be the context of a gateway which connections
  // its responsible for
  public Map<String, Connection> getClients();

  public void sendRemote(final Message msg) throws Exception;

  // FIXME - remove - not necessary - timeout implemented in waitForMsg
  // public Object sendBlockingRemote(final Message msg, Integer timeout) throws
  // Exception;

  public boolean isLocal(Message msg);

  /**
   * Generates a message to be sent to the runtime
   * instance at the given MRL instance. This message
   * invokes the runtime's {@link org.myrobotlab.service.Runtime#describe(String, DescribeQuery)}
   * method with a "fill-uuid" uuid parameter and a {@link DescribeQuery}
   * with the given connId.
   *
   * @param connId The UUID that is being connected to
   * @return A generated message that calls the remote runtime's {@code describe()} method
   */
  default Message getDescribeMsg(String connId) {
    // TODO support queries
    // FIXME !!! - msg.name is wrong with only "runtime" it should be
    // "runtime@id"
    // TODO - lots of options for a default "describe"

    return Message.createMessage(
            String.format("%s@%s", getName(), Runtime.get().getId()),
            "runtime",
            "describe",
            new Object[] {
                    CodecUtils.toJson(FILL_UUID_MAGIC_VAL),
                    CodecUtils.toJson(new DescribeQuery(Platform.getLocalInstance().getId(), connId))
            }
            );
  }

}
