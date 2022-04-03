package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.document.Classification;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Pid.PidOutput;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.service.interfaces.ComputerVision;
import org.myrobotlab.service.interfaces.PidControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * Tracking service which uses servos and opencv to track an object through a
 * video stream Its made specifically for servos, where a better solution would
 * be for a more general tracker to publish X,Y coordinates so that "any" type
 * of controller can track (e.g. motors)
 * 
 * @author GroG
 *
 */
public class Tracking extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Tracking.class);

  // FIXME - enable / disable
  List<String> trackingFilters = new ArrayList<>();

  // FIXME - should just be publishing a normalized x,y polar coordinate
  // and let the control system deal with that - having "servos" is
  // too device specific

  String pan;

  String tilt;

  String pid;

  String cv;

  long lostTrackingDelayMs = 1000;

  enum TrackingState {
    IDLE, SEARCHING, TRACKING
  }

  public static class Stats {
    public int inputCnt = 0;
    public float latency = 0;
  }

  TrackingState state = TrackingState.IDLE;

  Stats stats = new Stats();

  public Tracking(String n, String id) {
    super(n, id);

    registerForInterfaceChange(ServoControl.class);
    registerForInterfaceChange(PidControl.class);
    registerForInterfaceChange(ComputerVision.class);

    // defaults - pre getConfig or load ?
    trackingFilters.add("FaceDetectDNN");

    // publishes state/status info from this service
    addTask(1000, "publishStats");

  }

  /**
   * with input name route and attach by type - 2 instances of the same type
   * cannot be routed
   */
  public void attach(String name) {
    ServiceInterface si = Runtime.getService(name);
    if (si == null) {
      log.error("cannot attach by type - because {} currently does not exist", name);
      return;
    }
    if (si instanceof ComputerVision) {
      attachCv(name);
    } else if (si instanceof ServoControl) {
      error("must specify attachPan(ServoControl) or attachTilt(ServoControl");
    } else if (si instanceof PidControl) {
      attachPid(name);
    } else {
      error("%s does not know how to attach to %s of type %s", getName(), si.getName(), si.getSimpleName());
    }
    // attach by name ? e.g. .toLower.contains("pan") ???
  }

  public void attachPid(String pid) {
    this.pid = pid;
    subscribe(pid, "publishClassification");
  }

  /**
   * onCompute publish from pid - and routed/switched by key a more direct
   * approach might be to set a subscription for a specific key from the pid
   * controller directly to moveTo of a servo - similar to Joystick
   * 
   * send(listener.callbackName, listener.callbackMethod, data);
   * 
   * common patter is a message not broadcasted to all subscribers but "sent" to
   * a specific one and avoid the queue - joystick data, pin data, pid data all
   * are keyed and selectively listened to
   * 
   * @param pid
   */
  public void onPid(PidOutput pid) {
    if (state != TrackingState.IDLE) {
      if (pid.key.equals(pan)) {
        // send(pan, "moveIncr", pid.value);
        send(pan, "moveIncr", pid.value);
        // send(pan, "moveTo", pid.value + panRef.getCurrentInputPos());
        // log.warn("x {}", pid.value);
        // invokeOn(pan, "moveTo", pid.value);
      } else if (pid.key.equals(tilt)) {
        send(tilt, "moveIncr", pid.value);
        // send(tilt, "moveIncr", pid.value);
        // log.warn("y {}", pid.value);
        // invokeOn(tilt, "moveTo", pid.value);
      } else {
        warn("unknown onPid key %s", pid.key);
      }
    }
  }

  /**
   * verify all required service and configuration start tracking
   */
  public void enable() {
    if (pid == null) {
      error("pid not attached");
      return;
    }
    if (pan == null) {
      error("pan not attached");
      return;
    }
    if (tilt == null) {
      error("tilt not attached");
      return;
    }
    if (cv == null) {
      error("cv not attached");
      return;
    }
    // configuring pids
    // xPid.key = pan;
    // yPid.key = tilt;

    // send(pid, "addPid", xPid);
    // send(pid, "addPid", yPid);

    // preserve pre-existing filters ?
    // put back when disabled ?

    subscribe(pid, "publishPid");
    subscribe(cv, "publishClassification");
    for (String filter : trackingFilters) {
      send(cv, "addFilter", filter);
    }
    send(cv, "capture");
    state = TrackingState.SEARCHING;
    invoke("publishTrackingState");
    broadcastState();
  }

  @Deprecated /* DONT USE THIS ONLY FOR INTERNAL USE ! */
  public String setState(String state) {
    try {
      this.state = TrackingState.valueOf(state.toUpperCase());
      return state;
    } catch (Exception e) {
      error("%s not valid state", state);
    }
    return null;
  }

  public void onClassification(Map<String, List<Classification>> data) {
    stats.inputCnt++;

    if (state == TrackingState.IDLE) {
      // tracking was told to be idle
      // regardless of input - must return
      return;
    }

    for (String label : data.keySet()) {
      List<Classification> classifications = data.get(label);

      // FIXME - get largest bounding box - use that only
      for (Classification classification : classifications) {
        // log.info("found {}", classification);
        // get center of bounding box
        stats.latency = System.currentTimeMillis() - classification.getTs();

        double x = classification.getCenterX();
        double y = classification.getCenterY();

        log.info("input {} {}", x, y);

        // PV - measured value
        send(pid, "compute", pan, x);
        send(pid, "compute", tilt, y);

        // if largestFaceOnly

        if (state == TrackingState.SEARCHING) {
          // state transition - setup timer
          state = TrackingState.TRACKING;
          invoke("publishTrackingState");
        }

        purgeTask("publishLostTracking");
        addTaskOneShot(lostTrackingDelayMs, "publishLostTracking");

        break;
      }
    }
  }

  public TrackingState publishTrackingState() {
    log.warn("changing state to {}", state);
    return state;
  }

  public void publishLostTracking() {
    // FIXME - perhaps nice to publish tracking details of what was lost
    // this method is a call back from a timer & state transition from TRACKING
    // -to-> SEARCHING
    if (state == TrackingState.TRACKING) {
      // valid transition state
      state = TrackingState.SEARCHING;
    }
    invoke("publishTrackingState");
  }

  public Stats publishStats() {
    return stats;
  }

  public void disable() {
    state = TrackingState.IDLE;
    invoke("publishTrackingState");
  }

  public void attachPan(String pan) {
    this.pan = pan;
  }

  public void attachTilt(String tilt) {
    this.tilt = tilt;
  }

  public void attachCv(String cv) {
    this.cv = cv;
  }

  @Override
  public ServiceConfig getConfig() {

    TrackingConfig config = new TrackingConfig();
    // an interesting problem - the ui uses full name (rightfully so)
    // but local config should be short name
    // FIXME - the UI should determine if the two attaching services are local
    // to one another - if they are - then it should use shortnames
    config.cv = CodecUtils.shortName(cv);
    config.tilt = CodecUtils.shortName(tilt);
    config.pan = CodecUtils.shortName(pan);
    config.pid = CodecUtils.shortName(pid);
    config.enabled = (state == TrackingState.IDLE) ? false : true;
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    TrackingConfig config = (TrackingConfig) c;

    config.lostTrackingDelayMs = lostTrackingDelayMs;

    if (config.pan != null) {
      attachPan(config.pan);
    }
    if (config.tilt != null) {
      attachTilt(config.tilt);
    }
    if (config.pid != null) {
      attachPid(config.pid);
    }
    if (config.cv != null) {
      attachCv(config.cv);
    }

    if (config.enabled) {
      // enable();
    } else {
      disable();
    }
    return config;
  }

  public boolean isIdle() {
    return state == TrackingState.IDLE;
  }

  public void rest() {
    send(pan, "rest");
    send(tilt, "rest");
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // Runtime.saveDefault("Tracking");

      Runtime.start("intro", "Intro");
      Runtime.start("track", "Tracking");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      boolean done = true;
      if (done) {
        return;
      }

      // Pid2 pid = (Pid2) Runtime.start("pid", "Pid2");

      // pid.addPid("neck", 0.5, 1.0, 0.0, 240); // how does fractional Gain
      // mean
      // initial setpoint change ??

      // pid.addPid("rothead", 0.5, 1.0, 0.0, 320);

      // pid.addPid("neck", 1, 1, 0, 240);// why ???? 480 should be 240 ! /2
      // somewhere ?
      /*
       * Runtime.start("pan", "Servo"); Runtime.start(tiltName, "Servo");
       * Runtime.start("pid", "Pid"); Runtime.start("legacy", "Tracking");
       */
      // track.startWithDefaults();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
