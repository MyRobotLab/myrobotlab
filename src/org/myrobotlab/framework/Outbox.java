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

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.slf4j.Logger;

/*
 * Outbox is a message based thread which sends messages based on addListener lists and current
 * queue status.  It is only aware of the Service directory, addListener lists, and operators.
 * It can (if possible) take a message and move it to the inbox of a local service, or
 * (if necessary) send it to a local operator.
 * 
 * It knows nothing about protocols, serialization methods, or communication methods.
 */

public class Outbox implements Runnable, Serializable {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Outbox.class);

  static public final String RELAY = "RELAY";
  static public final String IGNORE = "IGNORE";
  static public final String BROADCAST = "BROADCAST";
  static public final String PROCESSANDBROADCAST = "PROCESSANDBROADCAST";

  NameProvider myService = null;
  LinkedList<Message> msgBox = new LinkedList<Message>();
  private boolean isRunning = false;
  private boolean blocking = false;
  int maxQueue = 1024;
  int initialThreadCount = 1;
  transient ArrayList<Thread> outboxThreadPool = new ArrayList<Thread>();

  public HashMap<String, ArrayList<MRLListener>> notifyList = new HashMap<String, ArrayList<MRLListener>>();
  CommunicationInterface comm = null;

  public Outbox(NameProvider myService) {
    this.myService = myService;
  }

  // TODO - config to put message in block mode - with no buffer overrun
  // TODO - config to drop message without buffer overrun e.g. like UDP
  public void add(Message msg) {
    // chase network bugs
    // log.error(String.format("%s.outbox.add(msg) %s.%s --> %s.%s",
    // myService.getName(), msg.sender, msg.sendingMethod, msg.name,
    // msg.method));
    synchronized (msgBox) {
      while (blocking && msgBox.size() == maxQueue) {
        // queue "full"
        try {
          // log.debug("outbox enque msg WAITING ");
          msgBox.wait(); // Limit the size
        } catch (InterruptedException ex) {
          log.debug("outbox add enque msg INTERRUPTED ");
        }
      }

      // we warn if over 10 messages are in the queue - but we will still
      // process them
      if (msgBox.size() > maxQueue) {
        log.warn(String.format("%s outbox BUFFER OVERRUN size %d", myService.getName(), msgBox.size()));
      }
      msgBox.addFirst(msg);

      // Logging.logTime(String.format("outbox %s size %d",myService.getName(),
      // msgBox.size()));

      if (log.isDebugEnabled()) {
        log.debug(String.format("msg [%s]", msg.toString()));
      }
      msgBox.notifyAll(); // must own the lock
    }
  }

  public CommunicationInterface getCommunicationManager() {
    return comm;
  }

  // FIXME - consider using a blocking queue now that we are using Java 5.0
  @Override
  public void run() {
    isRunning = true;
    while (isRunning) {
      Message msg = null;
      synchronized (msgBox) {
        try {
          while (msgBox.size() == 0) {
            // log.debug("outbox run WAITING ");
            msgBox.wait(); // must own the lock
          }
        } catch (InterruptedException ex) {
          log.debug("outbox run INTERRUPTED ");
          // msgBox.notifyAll();
          isRunning = false;
          return;
        }
        msg = msgBox.removeLast();
        // chase network bugs
        // log.error(String.format("%s.outbox.run(msg) %s.%s -- %s.%s ",
        // myService.getName(), msg.sender, msg.sendingMethod, msg.name,
        // msg.method));
        // log.debug(String.format("removed from msgBox size now %d",
        // msgBox.size()));
        msgBox.notifyAll();
      }

      // RELAY OTHER SERVICE'S MSGS
      // if the msg name is not my name - then
      // relay it
      // WARNING - broadcast apparently means name == ""
      // why would a message with my name be in my outbox ??? - FIXME
      // deprecate that logic
      if (msg.name != null) { // commented out recently -> &&
        // !myService.getName().equals(msg.name)
        log.debug("{} configured to RELAY ", msg.getName());
        comm.send(msg);
        // recently added -
        // if I'm relaying I'm not broadcasting...(i think)
        continue;
      }

      // BROADCASTS name=="" WILL DROP DOWN and be processed here
      if (notifyList.size() != 0) {
        // get the value for the source method
        ArrayList<MRLListener> subList = notifyList.get(msg.sendingMethod);
        if (subList == null) {
          log.debug(String.format("no static route for %s.%s ", msg.sender, msg.sendingMethod));
          // This will cause issues in broadcasts
          continue;
        }

        for (int i = 0; i < subList.size(); ++i) {
          MRLListener listener = subList.get(i);
          msg.name = listener.callbackName;
          msg.method = listener.callbackMethod;
          comm.send(msg);

          // must make new for internal queues
          // otherwise you'll change the name on
          // existing enqueued messages
          msg = new Message(msg);
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug(String.format("%s/%s(%s)", msg.getName(), msg.method, CodecUtils.getParameterSignature(msg.data) + " notifyList is empty"));
        }
        continue;
      }

    } // while (isRunning)
  }

  public void setCommunicationManager(CommunicationInterface c) {
    this.comm = c;
  }

  public int size() {
    return msgBox.size();
  }

  public void start() {
    for (int i = outboxThreadPool.size(); i < initialThreadCount; ++i) {
      Thread t = new Thread(this, myService.getName() + "_outbox_" + i);
      outboxThreadPool.add(t);
      t.start();
    }
  }

  public void stop() {
    isRunning = false;
    for (int i = 0; i < outboxThreadPool.size(); ++i) {
      Thread t = outboxThreadPool.get(i);
      t.interrupt();
      outboxThreadPool.remove(i);
      t = null;
    }
  }

  public LinkedList<Message> getMsgBox() {
    return msgBox;
  }

  public int getMaxQueueSize() {
    return maxQueue;
  }

  public boolean isBlocking() {
    return blocking;
  }

  public void setBlocking(boolean blocking) {
    this.blocking = blocking;
  }

  public boolean isRunning() {
    return isRunning;
  }

}
