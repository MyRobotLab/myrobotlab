/**
 *                    
 * @author grog (at) myrobotlab.org
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

// FIXME - should 'only' have jvm imports - no other dependencies or simple interface references
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

/**
 * @author GroG
 * 
 *         FIXME - either a structure interface or a typical java setter getter
 *         NO MIX !!
 * 
 */
public class Message implements Serializable {
  private static final long serialVersionUID = 1L;

  public final static String BLOCKING = "B";
  public final static String RETURN = "R";

  /**
   * unique identifier for this message
   */

  public long msgId;
  
  /**
   * the originating uri
   */
  public String uri;
  
  /**
   * apiKey related to data encoding
   */
  public String apiKey;
  
  /**
   * destination name of the message
   */
  public String name;
  /**
   * name of the sending Service which sent this Message
   */

  public String sender;
  /**
   * originating source method which generated this Message
   */
  public String sendingMethod;

  /**
   * history of the message, its routing stops and Services it passed through.
   * This is important to prevent endless looping of messages. Turns out
   * ArrayList is quicker than HashSet on small sets
   * http://www.javacodegeeks.com
   * /2010/08/java-best-practices-vector-arraylist.html
   */
  public HashSet<String> historyList;
  public HashMap<String, String> security;

  /**
   * status is currently used for BLOCKING message calls the current valid state
   * it can be in is null | BLOCKING | RETURN FIXME - this should be msgType not
   * status
   */

  public String status;

  public String msgType; // Broadcast|Blocking|Blocking Return - deprecated
  /**
   * the method which will be invoked on the destination @see Service
   */

  public String method;

  /**
   * the data which will be sent to the destination method data payload - if
   * invoking a service request this would be the parameter (list) - this would
   * the return type data if the message is outbound
   */
  public Object[] data;  

  public Message() {
    msgId = System.currentTimeMillis();
    name = new String(); // FIXME - allow NULL !
    sender = new String(); // FIXME - allow NULL !
    sendingMethod = new String();
    historyList = new HashSet<String>();
    method = new String();
  }

  public Message(final Message other) {
    set(other);
  }

  public Object[] getData() {
    return data;
  }

  public String getName() {
    return name;
  }

  final public void set(final Message other) {
    msgId = other.msgId;
    name = other.getName();
    sender = other.sender;
    sendingMethod = other.sendingMethod;
    // FIXED - not valid making a copy of a message
    // to send and copying there history list
    // historyList = other.historyList;
    historyList = new HashSet<String>();
    status = other.status;
    msgType = other.msgType;
    method = other.method;
    // you know the dangers of reference copy
    data = other.data;
  }

  final public void setData(Object... params) {
    this.data = params;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return CodecUtils.getMsgKey(this);
  }
  

  public static Message createMessage(NameProvider sender, String name, String method, Object[] data) {
    Message msg = new Message();
    msg.name = name; // destination instance name
    msg.sender = sender.getName();//this.getName();
    msg.data = data;
    msg.method = method;

    return msg;
  }

  
  static public Message createMessage(NameProvider sender, String name, String method, Object data) {
    if (data == null) {
      return createMessage(sender, name, method, null);
    }
    Object[] d = new Object[1];
    d[0] = data;
    return createMessage(sender, name, method, d);
  }




  public static void main(String[] args) throws InterruptedException {
    LoggingFactory.init(Level.DEBUG);

    Message msg = new Message();
    msg.method = "myMethod";
    msg.sendingMethod = "publishImage";
    msg.msgId = System.currentTimeMillis();
    msg.data = new Object[] { "hello" };

    /*
     * try { CodecUtils.toJsonFile(msg, "msg.xml"); } catch (Exception e) {
     * Logging.logError(e); }
     */
  }

}