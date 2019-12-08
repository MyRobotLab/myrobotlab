package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class WatchDogTimer extends Service {

  List<Message> globalActions = new ArrayList<Message>();

  /**
   * the timer class is a named watchdog timer which will look for resets at a
   * certain interval. If no checkpoint has been sent within a certain amount of
   * time, it will do a "corrective" action.
   *
   */
  public class Timer implements Runnable {

    transient WatchDogTimer parent;
    transient Thread myThread = null;

    String name;
    Integer interval = 2000;
    boolean active = false;
    Long futureTimeToAlertTs;
    Long lastCheckPointTs;

    Status alert;
    // Message correctiveAction;
    List<Message> actions = new ArrayList<Message>();

    /**
     * the default intereval which checkpoints are sent to the watchdog must be
     * < defaultIntervalMs
     */
    int sleepIntervalMs = 200;
    private boolean autoDeactivate = false;    

    public String toString() {
      return CodecUtils.toJson(this);
    }

    public Timer(WatchDogTimer parent, String checkPointName, String serviceName, String method, Object... data) {
      this.parent = parent;
      this.name = checkPointName;
      // this.futureTimeToAlertTs = futureTimeToAlertTs; this only gets set when
      // checkpoint is activated
      if (serviceName != null && method != null) {
        this.actions.add(Message.createMessage(getName(), serviceName, method, data));
      }
    }

    public void reset() {
      this.lastCheckPointTs = this.futureTimeToAlertTs;
      long currentTs = System.currentTimeMillis();
      this.futureTimeToAlertTs = currentTs + interval;
      log.debug("reset checkpoint {}.{} to +{} ms {} ms remaining", getName(), name, interval, lastCheckPointTs - currentTs);
    }

    @Override
    public void run() {
      try {
        futureTimeToAlertTs = System.currentTimeMillis() + interval;
        info("starting watchdog timer %s - expecting checkpoint in %d ms", name, interval);
        while (active) {

          long currentTime = System.currentTimeMillis();
          if (active && futureTimeToAlertTs < currentTime) {
            // we've passed our alert time - this checkpoint needs to alert !
            parent.invoke("publishAlert", this);

            // see if we should additionally fire a message
            for (Message action : actions) {
              log.info("watchdog {} apply corrective action {}", parent.getName(), action);
              action.historyList.clear();
              action.msgId = System.currentTimeMillis();
              parent.send(action);
            }

            for (Message globalAction : globalActions) {
              log.info("watchdog {} apply corrective global action {}", parent.getName(), globalAction);
              globalAction.historyList.clear();
              globalAction.msgId = System.currentTimeMillis();
              parent.send(globalAction);
            }

            // de-activate the alert
            if (autoDeactivate) {
              activate();
            }
          }
          sleep(sleepIntervalMs);
        }
      } catch (Exception e2) {
        log.error("watchdog threw", e2);
        deactivate();
      }
    }

    public void autoDeactivate(boolean b) {
      autoDeactivate = b;
    }

    public void activate() {
      if (myThread == null) {
        info("activating %s watchdog timer", name);
        futureTimeToAlertTs = System.currentTimeMillis() + interval;
        active = true;
        myThread = new Thread(this, String.format("%s.timer.%s", parent.getName(), name));
        myThread.start();
      }
    }

    public void deactivate() {
      if (myThread != null) {
        active = false;
        myThread = null;
      }
    }
  }

  /**
   * A check point worker sends messages to a timer to keep the watch dog from
   * doing the corrective action. Fully configurable for any rate, to be called
   * manually or in auto mode.
   *
   */
  public class CheckPointWorker implements Runnable {

    transient WatchDogTimer parent;

    transient Thread myThread = null;

    /**
     * interval to "send" a reset, reset includes how much to push (futureMs)
     * the timer for next reset
     */
    Integer checkPointIntervalMs = 200; // default
    String watchDogTimerName;
    String checkPointName;
    boolean active = false;

    public String toString() {
      return CodecUtils.toJson(this);
    }

    public CheckPointWorker(WatchDogTimer parent, Integer checkPointIntervalMs, String watchDogTimerName, String checkPointName) {

      this.parent = parent;
      if (checkPointIntervalMs != null) {
        this.checkPointIntervalMs = checkPointIntervalMs;
      }
      this.watchDogTimerName = watchDogTimerName;
      this.checkPointName = checkPointName;
    }

    @Override
    public void run() {
      try {
        while (active) {
          checkPoint(watchDogTimerName, checkPointName);
          sleep(checkPointIntervalMs);
        }
      } catch (Exception e2) {
        log.error("watchdog threw", e2);
        activate(false);
      }
    }

    public void activate(boolean b) {
      if (b) {
        if (myThread == null) {
          log.info("activating {} checkpoint worker", checkPointIntervalMs);
          active = true;
          myThread = new Thread(this, String.format("%s.%s.checkpoint", watchDogTimerName, checkPointName));
          myThread.start();
        }
      } else {
        if (myThread != null) {
          active = false;
          myThread = null;
        }
      }
    }
  }

  public final static Logger log = LoggerFactory.getLogger(WatchDogTimer.class);

  private static final long serialVersionUID = 1L;

  /**
   * all the checkpoints which this WatchDogTimer is watching
   */
  Map<String, Timer> timers = new HashMap<String, Timer>();

  Map<String, CheckPointWorker> checkpoints = new HashMap<String, CheckPointWorker>();

  /**
   * if not specified default interval is 2 seconds
   */
  int defaultIntervalMs = 2000;

  public WatchDogTimer(String n, String id) {
    super(n, id);
  }

  public Timer addTimer() {
    return addTimer(getName());
  }

  public Timer addTimer(String checkPointName) {
    Timer cp = addTimer(checkPointName, null, null, (Object[]) null);
    activateTimer(checkPointName);
    return cp;
  }

  public Timer addTimer(String checkPointName, String serviceName, String method) {
    return addTimer(checkPointName, serviceName, method, (Object[]) null);
  }

  public Timer addTimer(String checkPointName, String serviceName, String method, Object... data) {
    Timer timer = null;
    if (!timers.containsKey(checkPointName)) {
      timer = new Timer(this, checkPointName, serviceName, method, data);
      timers.put(checkPointName, timer);
    } else {
      timer = timers.get(checkPointName);
    }

    return timer;
  }

  public Timer activateTimer() {
    return activateTimer(getName());
  }

  public Timer activateTimer(String checkPointName) {
    if (timers.containsKey(checkPointName)) {
      Timer timer = timers.get(checkPointName);
      timer.activate();
      return timer;
    }
    error("cannot activate timer %s - not found", checkPointName);
    return null;
  }

  /**
   * default checkpoint sent
   */
  public void checkPoint() {
    checkPoint(getName(), getName());
  }

  /**
   * default method to "check-in" - a service calls this function to say
   * "everything is ok"
   * 
   * @param watchDogTimerName - name of timer
   */
  public void checkPoint(String watchDogTimerName) {
    checkPoint(watchDogTimerName, getName());
  }

  /**
   * named method to "check-in" - a service calls this function to say
   * "everything is ok"
   * 
   * @param checkPointName
   */

  public void checkPoint(String watchDogTimerName, String checkPointName) {
    send(watchDogTimerName, "onCheckPoint", checkPointName);
  }

  /**
   * named method to "check-in" - a service calls this function to say
   * "everything is ok" the next time it will "check-in" is in future
   * milliseconds
   * 
   * @param timerName - name of timer
   * @return - the timer
   */
  public Timer onCheckPoint(String timerName) {

    if (!timers.containsKey(timerName)) {
      error(String.format("checkpoint %s does not exist", timerName));
      return null;
    }

    Timer timer = timers.get(timerName);
    if (!timer.active) {
      return timer; // don't do anything if not activated
    }

    timer.reset();
    return timer;
  }

  public void onAlert(Status alert) {
    // code json
    log.info("alert sent ! {}", alert);
  }

  public Timer publishAlert(Timer alert) {
    info("watchdog alert ! %s ", alert);
    return alert;
  }

  public void stop() {
    for (Timer timer : timers.values()) {
      timer.deactivate();
    }

    for (CheckPointWorker worker : checkpoints.values()) {
      worker.activate(false);
    }
  }

  public void start() {
    for (Timer timer : timers.values()) {
      timer.activate();
    }

    for (CheckPointWorker worker : checkpoints.values()) {
      worker.activate(true);
    }
  }

  @Override
  public void stopService() {
    super.stopService();
    stop();
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(WatchDogTimer.class);
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("safety");
    return meta;
  }

  public void addCheckPoint(String watchDogName) {
    CheckPointWorker cpw = null;
    if (checkpoints.containsKey(watchDogName)) {
      cpw = checkpoints.get(watchDogName);
    } else {
      cpw = new CheckPointWorker(this, null, watchDogName, getName());
    }
    cpw.activate(true);
  }

  public void addAction(String service, String method, Object... params) {
    // if action is not assigned to a specific timer
    // add the action to "all"
    addGlobalAction(service, method, params);
    // addActionToTimer(getName(), service, method, params);
  }

  private void addGlobalAction(String service, String method, Object... params) {
    globalActions.add(Message.createMessage(getName(), service, method, params));
  }

  public void addActionToTimer(String timerName, String service, String method, Object... params) {
    Timer t = null;
    if (!timers.containsKey(timerName)) {
      t = addTimer(timerName);
    } else {
      t = timers.get(timerName);
    }
    t.actions.add(Message.createMessage(getName(), service, method, params));
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      {
        // most basic use case
        // single watchdog timer provides its own checkpoint
        // FIXME - junit which verifies checkpoint suppression is working

        Runtime.start("log", "Log");
        WatchDogTimer watchdog = (WatchDogTimer) Runtime.start("watchdog", "WatchDogTimer");
        watchdog.addTimer();
        watchdog.addAction("log", "info", "corrective action applied");

        for (int i = 0; i < 100; ++i) {
          watchdog.checkPoint();
        }

      }

      {
        // ============ wall-e raspi - watchdog e-power services ============

        // Mqtt mqtt01 = (Mqtt) Runtime.start("mqtt01", "Mqtt");
        WatchDogTimer watchdog = (WatchDogTimer) Runtime.start("watchdog", "WatchDogTimer");

        Motor m1 = (Motor) Runtime.start("m1", "Motor");

        // configuration
        // adding and activating a checkpoint
        // FIXME - junit "named" watchdogtimer
        watchdog.addTimer("joystickCheck"); // <-- response action
        // watchdog.addTimer("joystickCheck",
        // "serial01", "r1", 1);

        watchdog.addTimer("raspiCheck"); // <-- response action
        // watchdog.addTimer("joystickCheck",
        // "serial01", "r1", 1);
        watchdog.addAction("m1", "stop");
        // FIXME - activateAfterFirstCheckPoint
        // FIXME - reset
        // FIXME - action reset -> power stop & lock ?
        // mqtt01.connect("tcp://iot.eclipse.org:1883");
        // mqtt01.subscribe("mrl/broadcast/#");
      }
      {
        // ============ wall-e laptop - motor control services ============
        // Mqtt mqtt02 = (Mqtt) Runtime.start("mqtt02", "Mqtt");
        // mqtt02.connect("tcp://iot.eclipse.org:1883");
        // mqtt02.subscribe("mrl/broadcast/#");
        // Message msg = Message.createMessage(mqtt02.getName(), "runtime",
        // "hello", new Object[] { Runtime.getPlatform() });
        // FIXME - doesn't say "who" - should conform to gateway semantics
        // mqtt02.publish("mrl/broadcast/gateway01/runtime", 1,
        // CodecJson.encode(msg).getBytes());
      }
      {
        // ============ remote joystick ============
        // Mqtt mqtt03 = (Mqtt) Runtime.start("mqtt03", "Mqtt");
        // mqtt03.connect("tcp://iot.eclipse.org:1883");
        // mqtt03.subscribe("mrl/broadcast/#");
        WatchDogTimer joystickCheck = (WatchDogTimer) Runtime.start("joystickCheck", "WatchDogTimer");
        WatchDogTimer raspiCheck = (WatchDogTimer) Runtime.start("raspiCheck", "WatchDogTimer");

        // watchdog.activateTimer("motorCheck");

        // or "auto" mode can be set
        joystickCheck.addCheckPoint("watchdog");
        raspiCheck.addCheckPoint("watchdog");

        // FIXME - attach ? what to what - dual timer / echo ?

        // 2 second default - under two seconds is ok
        // not needed - have "auto" now
        /*
         * for (int i = 0; i < 100; ++i) { // "manual" checkpoints can be sent
         * by specifying // the name of the timer...
         * joystickCheck.checkPoint("watchdog"); //
         * motorCheck.checkPoint("watchdog"); sleep(100); }
         */
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
