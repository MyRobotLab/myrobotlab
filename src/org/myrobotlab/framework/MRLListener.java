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

package org.myrobotlab.framework;

import java.io.IOException;
import java.io.Serializable;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * MRLListner is a data object used to set up routes of messages within the
 * framework. It is sent to the service which a subscription to a Topic is
 * desired For details please refer to :
 * 
 * http://myrobotlab.org/content/myrobotlab-api
 * 
 * Typically this data class is used to send to a service on behalf of a
 * subscriptions. Its a subscription request data to add a message route from a
 * topic.
 * 
 * @author GroG
 *
 */
public final class MRLListener implements Serializable {
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(MRLListener.class);
  
  int _hashCode;

  /**
   * the keyed topic Method - when this method is invoked listeners are sent
   * messages with return data
   */
  public String topicMethod;

  /**
   * globally unique name of Service the a topic message will be sent to
   */
  public String callbackName;

  /**
   * the method which will be invoked
   */
  public String callbackMethod;

  public MRLListener(String topicMethod, String callbackName, String callbackMethod) {
    this.topicMethod = topicMethod;
    this.callbackMethod = callbackMethod;
    this.callbackName = callbackName;
  }

  final public boolean equals(final MRLListener other) {
    if (callbackName.equals(other.callbackName) && callbackMethod.equals(other.callbackMethod) && topicMethod.equals(other.topicMethod)) {
      return true;
    }
    return false;
  }

  
  @Override
  final public int hashCode() {
    if (_hashCode == 0) {
      _hashCode = 37 + topicMethod.hashCode() + callbackName.hashCode() + callbackMethod.hashCode();
    }

    return _hashCode;
  }
  

  @Override
  public String toString() {
    return String.format("%s -will activate-> %s.%s", topicMethod, callbackName, callbackMethod);
  }

  public static void main(String args[]) throws InterruptedException, IOException {
    LoggingFactory.init(Level.DEBUG);

    try {
      // MRLListener listener = new MRLListener("thrower/pitch");

      // assert listener.name = thrower
      // assert listener.outMethdod = onPitch
      // log.info(listener.toString());

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}