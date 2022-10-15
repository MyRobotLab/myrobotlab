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

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ClockConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.ClockEvent;
import org.slf4j.Logger;

/**
 * Clock - This is a simple clock service that can be started and stopped. It
 * generates a pulse with a timestamp on a regular interval defined by the
 * setInterval(Integer) method. Interval is in milliseconds.
 */
public class Clock extends Service {

  public class ClockThread implements Runnable {

    private transient Thread thread = null;

    public ClockThread() {
    }

    @Override
    public void run() {

      try {
        boolean firstTime = true;
        running = true;
        while (running) {
          Date now = new Date();
          Iterator<ClockEvent> i = events.iterator();
          while (i.hasNext()) {
            ClockEvent event = i.next();
            if (now.after(event.time)) {
              // TODO repeat - don't delete set time forward
              // interval
              send(event.name, event.method, event.data);
              i.remove();
            }
          }

          if (firstTime && skipFirst) {
            firstTime = false;
            continue;
          }
          invoke("pulse", now);
          invoke("publishTime", now);
          invoke("publishEpoch", now);

          Thread.sleep(interval);
          firstTime = false;
        }
      } catch (InterruptedException e) {
        log.info("ClockThread interrupt");
      }
      running = false;
      thread = null;
    }

    synchronized public void start() {
      if (thread == null) {
        thread = new Thread(this, getName() + "_ticking_thread");
        thread.start();
      } else {
        log.info("{} already started", getName());
      }
    }

    public void stop() {
      if (thread != null) {
        thread.interrupt();
        thread = null;
      } else {
        log.info("{} already stopped");
      }
    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Clock.class);

  protected volatile boolean running;

  final protected transient ClockThread myClock = new ClockThread();

  protected int interval = 1000;

  protected List<ClockEvent> events = new ArrayList<ClockEvent>();

  private boolean skipFirst = false;

  public Clock(String n, String id) {
    super(n, id);
  }

  public void addClockEvent(Date time, String name, String method, Object... data) {
    ClockEvent event = new ClockEvent(time, name, method, data);
    events.add(event);
  }

  // FIXME - to spec would be "publishClockStarted()"
  // clock started event
  public void publishClockStarted() {
    running = true;
    log.info("clock started");
    broadcastState();
  }

  /**
   * The clock was stopped event
   */
  public void publishClockStopped() {
    running = false;
    broadcastState();
  }

  /**
   * Date is published at an interval here
   * 
   * @param time
   *          t
   * @return t
   */
  public Date pulse(Date time) {
    return time;
  }

  public Date publishTime(Date time) {
    return time;
  }

  public long publishEpoch(Date time) {
    return time.getTime();
  }

  public void setInterval(Integer milliseconds) {
    interval = milliseconds;
    broadcastState();
  }

  public void startClock(boolean skipFirst) {
    this.skipFirst = skipFirst;
    myClock.start();
    invoke("publishClockStarted");
  }

  public void startClock() {
    startClock(false);
  }

  public boolean isClockRunning() {
    return running;
  }

  public void stopClock() {
    myClock.stop();
    broadcastState();
  }

  @Override
  public void stopService() {
    super.stopService();
    stopClock();
  }

  public Integer getInterval() {
    return interval;
  }

  @Override
  public ServiceConfig getConfig() {
    ClockConfig c = (ClockConfig) config;
    c.interval = interval;
    c.skipFirst = skipFirst;
    c.running = running;
    return c;
  }

  public ServiceConfig apply(ServiceConfig c) {
    ClockConfig config = (ClockConfig) c;
    if (config.running != null) {
      if (config.running) {
        startClock();
      } else {
        stopClock();
      }
    }
    interval = config.interval;
    skipFirst = config.skipFirst;
    return config;
  }

  public static void main(String[] args) throws Exception {
    try {
      // LoggingFactory.init(Level.WARN);

      Runtime runtime = Runtime.getInstance();

      Clock c1 = (Clock) Runtime.start("c1", "Clock");
      c1.startClock(true);
      c1.stopClock();

      boolean done = true;
      if (done) {
        return;
      }

      // connections
      boolean mqtt = true;
      boolean rconnect = false;

      /*
       * 
       * WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui"); //
       * webgui.setSsl(true); webgui.autoStartBrowser(false);
       * webgui.setPort(8887); webgui.startService();
       */
      if (mqtt) {
        // Mqtt mqtt02 = (Mqtt)Runtime.create("broker", "MqttBroker");
        Mqtt mqtt02 = (Mqtt) Runtime.start("mqtt02", "Mqtt");
        /*
         * mqtt02.setCert("certs/home-client/rootCA.pem",
         * "certs/home-client/cert.pem.crt", "certs/home-client/private.key");
         * mqtt02.connect(
         * "mqtts://a22mowsnlyfeb6-ats.iot.us-west-2.amazonaws.com:8883");
         */
        // mqtt02.connect("mqtt://broker.emqx.io:1883");
        mqtt02.connect("mqtt://localhost:1883");
      }

      if (rconnect) {
        // Runtime runtime = Runtime.getInstance();
        // runtime.connect("http://localhost:8888");

      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}