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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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

  HashMap<Long, Object[]> blockingList = new HashMap<Long, Object[]>();

  public Inbox() {
    this("Inbox");
  }

  public Inbox(String name) {
    this.name = name;
  }

  public void add(Message msg) {
    if ((msg.historyList.contains(name))) {
      log.error(String.format("* %s dumping duplicate message %s.%s msgid - %d %s", name, msg.name, msg.method, msg.msgId, msg.historyList));
      return;
    }

    msg.historyList.add(name);

    synchronized (msgBox) {
      while (blocking && msgBox.size() == maxQueue) // queue "full"
      {
        try {
          msgBox.wait();
        } catch (InterruptedException ex) {
          log.debug("inbox enque msg INTERRUPTED " + name);
        }
      }

      if (msgBox.size() > maxQueue) {
        bufferOverrun = true;
        log.warn(String.format("%s inbox BUFFER OVERRUN dumping msg size %d - %s", name, msgBox.size(), msg.method));
      } else {
        msgBox.addFirst(msg);
        // Logging.logTime(String.format("inbox - %s size %d", name,
        // msgBox.size()));
        if (log.isDebugEnabled()) {
          log.debug(String.format("%s.msgBox + 1 = %d", name, msgBox.size()));
        }
        msgBox.notifyAll(); // must own the lock
      }
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
   * @throws InterruptedException e
   * @see Message
   */
  public Message getMsg() throws InterruptedException {
    /*
     * TODO - remove below - Inbox will call switchboards
     * serializer/deserializer &amp; communicator send/recieve interface switchboard
     * has references to serializer and communicator - also all configuration
     * needed At this level ALL details on where the Message / Message came from
     * should be hidden and interfaces should be exposed only-
     */

    Message msg = null;

    synchronized (msgBox) {

      while (msg == null) { // while no messages && no messages that are
        // blocking
        if (msgBox.size() == 0) {
          // log.debug("Inbox WAITING " + name);
          msgBox.wait(); // must own the lock
        } else {
          msg = msgBox.removeLast();
          log.debug(String.format("%s.msgBox -1 %d", name, msgBox.size()));

          // --- sendBlocking support begin --------------------
          // TODO - possible safety check msg.status == Message.RETURN
          // &&
          if (blockingList.containsKey(msg.msgId)) {
            Object[] returnContainer = blockingList.get(msg.msgId);
            if (msg.data == null) // TODO - don't know if this is
            // correct but this works for
            // null data now
            {
              returnContainer[0] = null;
            } else {
              returnContainer[0] = msg.data[0]; // transferring
              // return data !
            }
            synchronized (returnContainer) {
              blockingList.remove(msg.msgId);
              returnContainer.notify(); // addListener sender
            }
            msg = null; // do not invoke this msg - sendBlocking has
            // been notified data returned
          }
          // --- sendBlocking support end --------------------

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

}
