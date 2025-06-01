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

package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// FIXME - should 'only' have jvm imports - no other dependencies or simple interface references
import org.myrobotlab.codec.CodecUtils;

/**
 * @author GroG
 * 
 *         FIXME - either a structure interface or a typical java setter getter
 *         NO MIX !!
 * 
 */
public class Message implements Serializable {

  private static final long serialVersionUID = 1L;

  // FIXME msgId should be a String encoded value of src and an atomic increment
  // ROS comes with a seq Id, a timestamp, and a frame Id
  /**
   * unique identifier for this message
   */

  public long msgId;

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
  protected List<String> historyList;

  /**
   * Meta data regarding the message - security, origin, and other information
   * not part of the message body Typically set at the gateway for remote origin
   * messages
   */
  protected HashMap<String, Object> properties;

  /**
   * status is currently used for BLOCKING message calls the current valid state
   * it can be in is null | BLOCKING | RETURN FIXME - this should be msgType not
   * status
   */

  public String status;

  public String encoding; // null == none |json|cli|xml|stream ...

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
    historyList = new ArrayList<String>();
    method = new String();
  }

  public Message(final Message other) {
    set(other);
  }

  public Object[] getData() {
    return data;
  }

  public String getName() {
    if (name == null) {
      return null;
    }
    int pos = name.indexOf("@");
    if (pos < 0) {
      return name;
    } else {
      return name.substring(0, pos);
    }
  }

  final public void set(final Message other) {
    msgId = other.msgId;
    name = other.name;
    sender = other.sender;
    sendingMethod = other.sendingMethod;
    // FIXMED AGAIN - 20210320 - it "is valid"
    // adding history is for sending remote - if we relay
    // we should add - and history should be checked for
    // loop back "from" remote
    // deep copy

    historyList = new ArrayList<String>();
    historyList.addAll(other.historyList);

    status = other.status;
    encoding = other.encoding;
    method = other.method;
    // you know the dangers of reference copy
    // shallow data copy
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

  public static Message createMessage(String sender, String name, String method, Object[] data) {
    Message msg = new Message();
    msg.name = name; // destination instance name
    msg.sender = sender;
    msg.data = data;
    msg.method = method;

    return msg;
  }

  static public Message createMessage(String sender, String name, String method, Object data) {
    if (data == null) {
      return createMessage(sender, name, method, null);
    }
    return createMessage(sender, name, method, new Object[] { data });
  }

  public static void main(String[] args) throws InterruptedException {

    Message msg = new Message();
    msg.method = "myMethod";
    msg.sendingMethod = "publishImage";
    msg.msgId = System.currentTimeMillis();
    msg.data = new Object[] { "hello" };

  }

  public Object getProperty(String key) {
    if (properties == null || !properties.containsKey(key)) {
      return null;
    }
    return properties.get(key);
  }

  public void setProperty(String key, Object value) {
    if (properties == null) {
      properties = new HashMap<>();
    }
    properties.put(key, value);
  }

  public void putAll(Map<String, Object> props) {
    props.putAll(props);
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public String getId() {
    if (name == null) {
      return null;
    }
    int p = name.indexOf("@");
    if (p > 0) {
      return name.substring(p + 1);
    }
    return null;
  }

  public String getSrcId() {
    int pos = sender.indexOf("@");
    if (pos > 0) {
      return sender.substring(pos + 1);
    }
    return null;
  }

  public String getSrcName() {
    if (sender == null) {
      return null;
    }
    int pos = sender.indexOf("@");
    if (pos > 0) {
      return sender.substring(0, pos);
    }
    return sender;
  }

  public String getFullName() {
    return name;
  }

  public void addHop(String id) {
    historyList.add(id);
  }

  public boolean containsHop(String id) {
    for (String travelled : historyList) {
      if (travelled.contains(id)) {
        return true;
      }
    }
    return false;
  }

  public void clearHops() {
    historyList = new ArrayList<>();
  }

  public List<String> getHops() {
    return historyList;
  }

  public String getSrcFullName() {
    return sender;
  }

  public long getMsgId() {
    return msgId;
  }

  public String getMethod() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Message message = (Message) o;
    return msgId == message.msgId
            && Objects.equals(name, message.name)
            && Objects.equals(sender, message.sender)
            && Objects.equals(sendingMethod, message.sendingMethod)
            && Objects.equals(historyList, message.historyList)
            && Objects.equals(properties, message.properties)
            && Objects.equals(status, message.status)
            && Objects.equals(encoding, message.encoding)
            && Objects.equals(method, message.method)
            && Arrays.deepEquals(data, message.data);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(
                    msgId, name, sender,
                    sendingMethod, historyList,
                    properties, status, encoding,
                    method
    );
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }
}