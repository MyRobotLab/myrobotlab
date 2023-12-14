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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.Gateway;
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

  protected String name = null;

  private transient LinkedList<Message> msgBox = new LinkedList<Message>();

  private boolean isRunning = false;

  private boolean blocking = false;

  int maxQueue = 1024;

  int initialThreadCount = 1;

  transient ArrayList<Thread> outboxThreadPool = new ArrayList<Thread>();

  protected Map<String, FilterInterface> filters = new HashMap<>();

  public interface FilterInterface {
    public boolean filter(Message msg);
  }

  /**
   * pub/sub listeners - HashMap &lt; {topic}, List {listeners} &gt;
   */
  protected Map<String, List<MRLListener>> notifyList = new HashMap<String, List<MRLListener>>();

  List<MessageListener> listeners = new ArrayList<MessageListener>();

  private boolean autoClean = false;

  public boolean isAutoClean() {
    return autoClean;
  }

  public void setAutoClean(boolean autoClean) {
    this.autoClean = autoClean;
  }

  public Outbox(String myService) {
    this.name = myService;
  }

  public Set<String> getAttached(String publishingPoint) {
    return getAttached(publishingPoint, true);
  }

  public Set<String> getAttached(String publishingPoint, boolean localOnly) {
    Set<String> unique = new TreeSet<>();
    for (List<MRLListener> subcribers : notifyList.values()) {
      for (MRLListener listener : subcribers) {
        if (localOnly && !CodecUtils.isLocal(listener.callbackName)) {
          continue;
        }
        if (publishingPoint == null) {
          unique.add(listener.callbackName);
        } else if (listener.topicMethod.equals(publishingPoint)) {
          unique.add(listener.callbackName);
        }
      }
    }
    return unique;
  }

  // TODO - config to put message in block mode - with no buffer overrun
  // TODO - config to drop message without buffer overrun e.g. like UDP
  public void add(Message msg) {
    // chase network bugs
    // log.error(String.format("%s.outbox.add(msg) %s.%s --> %s.%s",
    // myService.getName(), msg.sender, msg.sendingMethod, msg.name,
    // msg.method));
    synchronized (msgBox) {
      while (blocking && (msgBox.size() >= maxQueue)) {
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
        // log.warn("{} outbox BUFFER OVERRUN size {} Dropping message to
        // {}.{}", myService.getName(), msgBox.size(), msg.name, msg.method);
        log.warn("{} outbox BUFFER OVERRUN size {} Dropping message to {}", name, msgBox.size(), msg);
      }
      msgBox.addFirst(msg);

      // Logging.logTime(String.format("outbox %s size %d",myService.getName(),
      // msgBox.size()));

      if (log.isDebugEnabled()) {
        log.debug("msg [{}]", msg.toString());
      }
      msgBox.notifyAll(); // must own the lock
    }

    // now that it's actually in the queue. let's notify the listeners
    for (MessageListener ml : listeners) {
      ml.onMessage(msg);
    }
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
      if (msg.getName() != null) {
        log.debug("{} configured to RELAY ", msg.getName());
        send(msg);
        // recently added -
        // if I'm relaying I'm not broadcasting...(i think)
        continue;
      }

      // BROADCASTS name=="" WILL DROP DOWN and be processed here
      if (notifyList.size() != 0) {
        // get the value for the source method
        List<MRLListener> subList = notifyList.get(msg.sendingMethod);
        if (subList == null) {
          // log.debug("no additional routes for {}.{} ", msg.sender, msg.sendingMethod);
          // This will cause issues in broadcasts
          continue;
        }

        for (int i = 0; i < subList.size(); ++i) {
          MRLListener listener = subList.get(i);
          msg.setName(listener.callbackName);
          msg.method = listener.callbackMethod;

          if (!isFiltered(msg)) {
            send(msg);
          }

          // must make new for internal queues
          // otherwise you'll change the name on
          // existing enqueued messages
          msg = new Message(msg);
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug("{}/{}({}) notifyList is empty", msg.getName(), msg.method,
              CodecUtils.getParameterSignature(msg.data));
        }
        continue;
      }
    } // while (isRunning)
  }

  public FilterInterface addFilter(String name, String method, FilterInterface filter) {
    return filters.put(String.format("%s.%s", CodecUtils.getFullName(name), method), filter);
  }

  public FilterInterface removeFilter(String name, String method) {
    return filters.remove(String.format("%s.%s", CodecUtils.getFullName(name), method));
  }

  public boolean isFiltered(Message msg) {
    String fullname = CodecUtils.getFullName(msg.name);
    if (filters.size() == 0 || !filters.containsKey(String.format("%s.%s", fullname, msg.method))) {
      return false;
    } else {
      return filters.get(String.format("%s.%s", fullname, msg.method)).filter(msg);
    }
  }

  public int size() {
    return msgBox.size();
  }

  public void start() {
    for (int i = outboxThreadPool.size(); i < initialThreadCount; ++i) {
      Thread t = new Thread(this, CodecUtils.getShortName(name) + "_outbox_" + i);
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

  public void setMaxQueueSize(int size) {
    maxQueue = size;
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

  public void addMessageListener(MessageListener ml) {
    // already attached.
    if (listeners.contains(ml))
      return;
    listeners.add(ml);
  }

  final public void send(final Message msg) {

    try {

      Runtime runtime = Runtime.getInstance();

      if (runtime.isLocal(msg)) {
        // should it invoke(potentially block) or conventionally input on in
        // queue
        // ?
        ServiceInterface sw = Runtime.getService(msg.getName());
        if (sw == null && autoClean) {
          log.warn("could not find service {} to process {} from sender {} - tearing down route", msg.getName(),
              msg.method, msg.sender);
          ServiceInterface sender = Runtime.getService(msg.sender);
          if (sender != null) {
            sender.removeListener(msg.sendingMethod, msg.getName(), msg.method);
          }
          return;
        } else if (sw == null) {
          log.info("could not find service {} to process {} from sender {}", msg.getName(), msg.method, msg.sender);
          return;
        }

        // if service is local - give it to that service's inbox
        URI host = sw.getInstanceId();
        if (host == null) {
          sw.in(msg);
        }
      } else {
        // get gateway
        Gateway gateway = Runtime.getInstance().getGatway(msg.getId());
        if (gateway == null) {
          // log.error("gateway not found for msg.id {} {}", msg.getId(), msg);
          return;
        }
        gateway.sendRemote(msg);
      }

    } catch (Exception e) {
      log.error("outbox threw", e);
    }
  }

  /**
   * remove ALL listeners/subscribers
   */
  public void reset() {
    notifyList = new HashMap<String, List<MRLListener>>();
  }

  /**
   * Safe detach for single subscriber
   * 
   * @param service
   *                the name of the listener to detach
   * 
   */
  synchronized public void detach(String service) {
    String name = CodecUtils.getFullName(service);
    for (String topic : notifyList.keySet()) {
      List<MRLListener> subscribers = notifyList.get(topic);
      ArrayList<MRLListener> smallerList = new ArrayList<>();
      for (MRLListener listener : subscribers) {
        if (!listener.callbackName.equals(name)) {
          smallerList.add(listener);
        }
      }
      notifyList.put(topic, smallerList);
    }
  }

  public Map<String, List<MRLListener>> getNotifyList() {
    return notifyList;
  }
}
