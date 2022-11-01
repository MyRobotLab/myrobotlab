/**
 *                    
 * @author grog (at) myrobotlab.org
 * 
 * */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ClockConfig;
import org.myrobotlab.service.config.ServiceConfig;
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
      ClockConfig c = (ClockConfig) config;

      try {

        c.running = true;
        while (c.running) {
          Thread.sleep(c.interval);
          Date now = new Date();
          for (Message msg : events) {
            send(msg);
          }
          invoke("pulse", now);
          invoke("publishTime", now);
          invoke("publishEpoch", now);
        }
      } catch (InterruptedException e) {
        log.info("ClockThread interrupt");
      }
      c.running = false;
      thread = null;
    }

    // FIXME - synchronized methods is silly here - access needs to be
    // synchronized "between" start & stop
    // TODO - create and use a single thread - use wait(sleep) notify for
    // control
    synchronized public void start() {
      if (thread == null) {
        thread = new Thread(this, getName() + "_ticking_thread");
        thread.start();
        invoke("publishClockStarted");
      } else {
        log.info("{} already started", getName());
      }
    }

    synchronized public void stop() {
      ClockConfig c = (ClockConfig) config;
      if (thread != null) {
        thread.interrupt();
      } else {
        log.info("{} already stopped");
      }

      // change state - broadcast it
      if (c.running == true) {
        broadcastState();
      }

      c.running = false;
      thread = null;
    }
  }

  private static final long serialVersionUID = 1L;

  final public static Logger log = LoggerFactory.getLogger(Clock.class);

  final protected transient ClockThread myClock = new ClockThread();

  /**
   * list of messages the clock can send - these are set with addClockEvent
   */
  final protected List<Message> events = new ArrayList<>();

  public Clock(String n, String id) {
    super(n, id);
  }

  public void addClockEvent(String name, String method, Object... data) {
    Message event = Message.createMessage(getName(), name, method, data);
    events.add(event);
  }

  /**
   * clears all the clock events
   */
  public void clearClockEvents() {
    events.clear();
  }

  /**
   * event published for when the clock is started
   */
  public void publishClockStarted() {
    log.info("clock started");
    broadcastState();
  }

  /**
   * the clock was stopped event
   */
  public void publishClockStopped() {
    log.info("clock stopped");
    broadcastState();
  }

  /**
   * date is published at an interval here
   * 
   * @param time
   *          t
   * @return t
   */
  @Deprecated /* use publishTime or preferably publishEpoch as epoch is in a useful millisecond value */
  public Date pulse(Date time) {
    return time;
  }

  /**
   * publishing point for a the current date object
   * @param time
   * @return
   */
  public Date publishTime(Date time) {
    return time;
  }

  /**
   * publishing point for epoch
   * @param time - epoch value, number of milliseconds from Jan 1 1970
   * @return
   */
  public long publishEpoch(Date time) {
    return time.getTime();
  }

  /**
   * set the interval of clock events to the current millisecond value
   * @param milliseconds
   */
  public void setInterval(Integer milliseconds) {
    ClockConfig c = (ClockConfig) config;
    c.interval = milliseconds;
    broadcastState();
  }

  @Deprecated /* use startClock skipFirst is default behavior */
  public void startClock(boolean skipFirst) {
    startClock()
  }

  /**
   * start the clock
   */
  public void startClock() {
    myClock.start();
  }

  /**
   * see if the clock is running
   * @return
   */
  public boolean isClockRunning() {
    ClockConfig c = (ClockConfig) config;
    return c.running;
  }

  /**
   * stop a clock
   */
  public void stopClock() {
    myClock.stop();
  }

  @Override
  public void stopService() {
    super.stopService();
    stopClock();
  }

  /**
   * return the current interval in milliseconds
   * @return
   */
  public Integer getInterval() {
    return ((ClockConfig) config).interval;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    super.apply(c);
    ClockConfig config = (ClockConfig) c;
    if (config.running != null) {
      if (config.running) {
        startClock();
      } else {
        stopClock();
      }
    }
    return config;
  }

  public void restartClock() {
    stopClock();
    startClock();
  }

  public static void main(String[] args) throws Exception {
    try {

      Runtime.start("webgui", "WebGui");

      Clock c1 = (Clock) Runtime.start("c1", "Clock");
      c1.startClock();
      c1.stopClock();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}