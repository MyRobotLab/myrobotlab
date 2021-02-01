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

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
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
      thread = new Thread(this, getName() + "_ticking_thread");
      thread.start();
    }

    @Override
    public void run() {

      try {
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

          if (!NoExecutionAtFirstClockStarted) {
            invoke("pulse", new Date());
            invoke("publishTime", new Date());
          }
          Thread.sleep(interval);
          NoExecutionAtFirstClockStarted = false;
        }
      } catch (InterruptedException e) {
        log.info("ClockThread interrupt");        
      }
      running = false;
    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Clock.class);
  
  public volatile boolean running;

  public int interval = 1000;

  protected transient ClockThread myClock = null;

  // FIXME
  protected ArrayList<ClockEvent> events = new ArrayList<ClockEvent>();

  private boolean NoExecutionAtFirstClockStarted = false;

  private boolean restartMe;

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

  public void publishClockStopped() {
    running = false;
    broadcastState();
    if (restartMe) {
      sleep(10);
      startClock(NoExecutionAtFirstClockStarted);
    }
  }

  public Date pulse(Date time) {
    return time;
  }

  public Date publishTime(Date time) {
    return time;
  }

  public void setInterval(Integer milliseconds) {
    interval = milliseconds;
    broadcastState();
  }

  public void startClock(boolean NoExecutionAtFirstClockStarted) {
    if (myClock == null) {
      this.NoExecutionAtFirstClockStarted = NoExecutionAtFirstClockStarted;
      // info("starting clock");
      myClock = new ClockThread();
      invoke("publishClockStarted");
    } else {
      log.info("clock already started");
    }
  }

  public void restartClock(boolean NoExecutionAtFirstClockStarted) {
    this.NoExecutionAtFirstClockStarted = NoExecutionAtFirstClockStarted;
    if (!running) {
      startClock(NoExecutionAtFirstClockStarted);
    } else {
      stopClock(true);
    }

  }

  public void startClock() {
    startClock(false);
  }

  public void restartClock() {
    restartClock(false);
  }

  public void stopClock() {
    stopClock(false);
  }

  public void stopClock(boolean restartMe) {
    this.restartMe = restartMe;
    if (myClock != null) {
      // info("stopping clock");
      log.info("stopping " + getName() + " myClock");
      myClock.thread.interrupt();
      myClock.thread = null;
      myClock = null;
      // have requestors broadcast state !
      // broadcastState();
      invoke("publishClockStopped");
    } else {
      log.info("clock already stopped");
    }
    running = false;
    broadcastState();
  }

  @Override
  public void stopService() {
    super.stopService();
    stopClock();
  }

  public static void main(String[] args) throws Exception {
    LoggingFactory.init(Level.DEBUG);
    Runtime.main(new String[] { "--id", "r7", "--log-level", "DEBUG" });
    Clock clock = (Clock) Runtime.start("clock02", "Clock");
    Runtime runtime = Runtime.getInstance();
    runtime.connect("http://admin.local:8888");
    /*
     * clock.setInterval(1000); clock.restartClock(); sleep(2000);
     * clock.restartClock(); sleep(2000); clock.stopClock();
     */
  }

}