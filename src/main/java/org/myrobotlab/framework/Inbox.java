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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Inbox implements Serializable {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Inbox.class.getCanonicalName());

  String name;
  LinkedList<Message> msgBox = new LinkedList<Message>();
  boolean isRunning = false;
  boolean bufferOverrun = false;
  boolean blocking = false;
  int maxQueue = 1024; // will need to adjust unit test if you change this
  // value

  // support remote blocking... in-process blocking uses invoke
  public HashMap<String, Object[]> blockingList = new HashMap<>();

  List<MessageListener> listeners = new ArrayList<MessageListener>();

  public Inbox() {
    this("Inbox");
  }

  public Inbox(String name) {
    this.name = name;
  }

  public void add(Message msg) {
    /**
     * <pre>
     * Trying to only make this applicable for hops dealing with remote messages
     * - not local one ! if ((msg.historyList.contains(name))) { log.error("* {}
     * dumping duplicate message {}.{} msgid - {} {}", name, msg.getName(),
     * msg.method, msg.msgId, msg.historyList); return; }
     * 
     * msg.historyList.add(name);
     */

    // --- sendBlocking support begin --------------------
    // TODO - possible safety check msg.status == Message.RETURN
    // &&
    String blockingKey = String.format("%s.%s", msg.getFullName(), msg.getMethod());
    if (blockingList.containsKey(blockingKey)) {
      Object[] returnContainer = blockingList.get(blockingKey);
      if (msg.data == null) {
        returnContainer[0] = null;
      } else {
        // transferring data
        returnContainer[0] = msg.data[0];
      }

      synchronized (returnContainer) {
        blockingList.remove(blockingKey);
        returnContainer.notifyAll(); // addListener sender
      }
      // do not invoke this msg - sendBlocking has
      // been notified and data returned
      // --- sendBlocking support end --------------------
    } else {
      // We do want to invoke this message
      synchronized (msgBox) {
        while (blocking && (msgBox.size() >= maxQueue)) // queue "full"
        {
          try {
            msgBox.wait();
          } catch (InterruptedException ex) {
            log.debug("inbox enque msg INTERRUPTED " + name);
          }
        }

        if (msgBox.size() > maxQueue) {
          bufferOverrun = true;
          log.warn("inbox size {} BUFFER OVERRUN dumping msg from {} To {}.{}", msgBox.size(), msg.sender, msg.name, msg.method);
        } else {
          msgBox.addFirst(msg);
          // Logging.logTime(String.format("inbox - %s size %d", name,
          // msgBox.size()));
          if (log.isDebugEnabled()) {
            log.debug("{}.msgBox + 1 = {}", name, msgBox.size());
          }
          msgBox.notifyAll(); // must own the lock
        }
      }
    }

    // Even if message is a blocking return, we still want to notify (right?)
    // TODO: move this to a base class Inbox/Outbox are very similar.
    // now that it's actually in the queue. let's notify the listeners
    for (MessageListener ml : listeners) {
      ml.onMessage(msg);
    }

  }

  public void clear() {
    msgBox.clear();
  }

  // FIXME - implement with HashSet or HashMap !!!!
  // ******* TEST WITHOUT DUPE CHECKING *********
  public boolean duplicateMsg(ArrayList<RoutingEntry> history) {

    for (int i = 0; i < history.size(); ++i) {
      if (history.get(i).name.equals(name)) {
        log.error("dupe message {} {}", name, history);

        return true;
      }
    }

    return false;
  }

  /**
   * Blocks and waits on a message put on the queue of the InBox. Service
   * default behavior will wait on getMsg for a message, when they recieve a
   * message they invoke it.
   * 
   * @return the Message on the queue
   * @throws InterruptedException
   *           e
   * @see Message
   */
  public Message getMsg() throws InterruptedException {
    /*
     * TODO - remove below - Inbox will call switchboards
     * serializer/deserializer &amp; communicator send/recieve interface
     * switchboard has references to serializer and communicator - also all
     * configuration needed At this level ALL details on where the Message /
     * Message came from should be hidden and interfaces should be exposed only-
     */

    Message msg = null;

    synchronized (msgBox) {

      while (msg == null) { // while no messages && no messages that are
        // blocking
        if (msgBox.size() == 0) {
          msgBox.wait(); // must own the lock
        } else {
          msg = msgBox.removeLast();
          log.debug("{}.msgBox -1 {}", name, msgBox.size());
        }
      }
      msgBox.notifyAll();
    }
    return msg;
  }

  public boolean isBufferOverrun() {
    return bufferOverrun;
  }

  public void setBlocking(boolean toBlock) {
    blocking = toBlock;
  }

  public int size() {
    return msgBox.size();
  }

  public void addMessageListener(MessageListener ml) {
    // already attached.
    if (listeners.contains(ml))
      return;
    listeners.add(ml);
  }

}