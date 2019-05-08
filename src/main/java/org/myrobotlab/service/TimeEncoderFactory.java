package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoControlListener;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Service to encode a stream of pulses (absolute or relative) for other
 *         services. This can handle multiple streams of pulses to multiple
 *         services - ie typically only one is needed for all TimeEncoding
 *         purposes.
 * 
 *         Because this is implemented without hardware it does not need to be
 *         attached to a EncoderController
 * 
 *         FIXME - make a "nonService" EncoderControl == Timer such that this
 *         service manages them all ! e.g. an EncoderFactory
 * 
 * 
 *         Speed is always in degrees per ms Average servo speed is 0.5
 *         degrees/ms
 *
 */
public class TimeEncoderFactory extends Service implements EncoderControl, ServoControlListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TimeEncoderFactory.class);

  Map<String, EncoderListener> listeners = new HashMap<>();

  Map<String, Timer> encoders = new HashMap<>();

  /**
   * TimeEncoder - a universal time encoder used for doing estimations and
   * planning of trajectories and paths. It works on a persistent thread put in
   * a wait state, waiting for requested estimations.
   * 
   * @author GroG
   * 
   *         FIXME controlled clear units (degrees cm per s vs ms .. etc)
   *
   */
  public class Timer implements Runnable {

    // FIXME - remove type specific references
    EncoderListener servo = null;

    boolean isRunning = false;
    transient Thread myThread = null;

    // default max speed
    // Common servos have operating speeds in the range of 0.05 to 0.2 s/60
    // degree.
    // going with 60 degrees in 0.12 s
    //

    // default max speed
    double defaultMaxSpeedDegreesPerMs = 0.5;

    // time between next evaluation
    int sampleIntervalMs = 5;

    // where we are
    double beginPos;

    // where we want to go
    double targetPos;

    // the distance to get there (degrees)
    double distance;

    // the speed (degrees per sec)?
    double tspeed;

    // FIXME - work this out...
    double speedDegreesPerMs;

    // time of move in ms
    double moveTimeMs;

    // timestamp of the beginning of the move
    long beginMoveTs;

    // estimate timestamp of the end of the move
    long endMoveTs;

    // our estimated position
    double estimatedPos;

    // current time
    long now;

    // name of encoder data source
    String name;

    boolean autoProcess = true;
    
    boolean enabled = true;

    public Timer(ServoControl servo) {
      this.servo = servo;
      myThread = new Thread(this, String.format("%s.%s.time-encoder", servo.getName(), getName()));
      myThread.start();
    }

    // TODO - manage other types .. motors .. joysticks ... etc..
    // FIXME - generalize inputs so that no full control type is known - e.g. calculateTrajectory(beginPos, targetPos, tspeed)
    /*
    public void calculateTrajectory(ServoControl servo) {
      // find current distance - // make a plan ...
      beginPos = servo.getPos();
      targetPos = servo.getTargetPos();
      distance = servo.getTargetPos() - servo.getPos();
      // always positive ? :P
      tspeed = (servo.getSpeed() == null) ? defaultMaxSpeedDegreesPerMs : servo.getSpeed() / 1000; 
      speedDegreesPerMs = (beginPos > targetPos) ? -1 * tspeed : tspeed;

      moveTimeMs = Math.abs(distance / speedDegreesPerMs);
      beginMoveTs = System.currentTimeMillis();
      endMoveTs = beginMoveTs + (long) moveTimeMs;

      log.info("{}", this);

      estimatedPos = servo.getPos();

      // while not done ... do
      // leave if timets > endMoveTs or if canceled with new move
      // while()
      now = beginMoveTs;
      name = servo.getName();
      
      if (autoProcess) {
        processTrajectory(name);
      }
    }
    */

    // TODO - cool this works deprecate other
    public void calculateTrajectory(double inBeginPos, double inTargetPos, Double inSpeed) {
      // find current distance - // make a plan ...
      beginPos = inBeginPos;
      targetPos = inTargetPos;
      distance = targetPos - beginPos;
      // always positive ? :P
      tspeed = (inSpeed == null) ? defaultMaxSpeedDegreesPerMs : inSpeed / 1000; // FIXME UNITS !!!! - for ms
      speedDegreesPerMs = (beginPos > targetPos) ? -1 * tspeed : tspeed;

      moveTimeMs = Math.abs(distance / speedDegreesPerMs);
      beginMoveTs = System.currentTimeMillis();
      endMoveTs = beginMoveTs + (long) moveTimeMs;

      log.info("{}", this);

      estimatedPos = inBeginPos;

      // while not done ... do
      // leave if timets > endMoveTs or if canceled with new move
      // while()
      now = beginMoveTs;
      name = servo.getName();
      
      if (autoProcess) {
        processTrajectory(name);
      }
    }

    
    // TODO - processTrajectory()
    void processTrajectory(String name) {
        Timer timer = encoders.get(name);
        if (timer == null) {
          log.error("unknown timer {}", name);
        }
        synchronized (timer) { // TODO - change to "this" when not a service
          // this starts the time encoder thread waiting for move commands
          // to start the process of processing a "planned move" sequence
          timer.notifyAll();
        }
    }

    @Override
    public void run() {
      try {

        isRunning = true;
        while (isRunning) {
          // wait for next move ...
          synchronized (this) {
            this.wait();
          }

          if (speedDegreesPerMs == 0) { 
            // FIXME may need some adjustment in this - should a stop event with 0 pos change be sent ? 
            log.info("speed is 0 - not moving");
            continue;
          }

          while (now < endMoveTs) {

            now = System.currentTimeMillis();
            // speed has +/- direction
            estimatedPos = beginPos + speedDegreesPerMs * (now - beginMoveTs);

            if (beginPos < targetPos && estimatedPos > targetPos) {
              estimatedPos = targetPos;
            }
            if (beginPos > targetPos && estimatedPos < targetPos) {
              estimatedPos = targetPos;
            }

            // log.info(String.format("new pos %.2f", estimatedPos)); helpful to
            // debug
            EncoderData d = new EncoderData(name, null, estimatedPos);
            servo.onEncoderData(d);
            sleep(sampleIntervalMs);
          }
          
          // when we are leaving - its a "finished move"
          // log.info("finished moved");
          EncoderData d = new EncoderData(name, null, estimatedPos);
          servo.onEncoderData(d);
          
        }
      } catch (InterruptedException e) {
        log.info("stopping TimeEncoder Timer ...");
      }
    }

    public void stop() {
      isRunning = false;
      myThread.interrupt();
    }

    public String toString() {
      return String.format("@ ts %d starting at position %.1f %s will travel %.1f degrees to position %.1f in %.1f ms ending at %d ts", beginMoveTs, beginPos, name, distance, targetPos,
          moveTimeMs, endMoveTs);
    }

  }

  public TimeEncoderFactory(String n) {
    super(n);
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

    ServiceType meta = new ServiceType(TimeEncoderFactory.class);
    meta.addDescription("general purpose timing encoder used in place of real hardware");
    meta.setAvailable(true);
    meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  /**
   * overloaded routing attach
   */
  public void attach(Attachable service) throws Exception {
    if (ServoControl.class.isAssignableFrom(service.getClass())) {
      attach((ServoControl) service);
    } else {
      warn(String.format("%s.attach does not know how to attach to a %s", this.getClass().getSimpleName(), service.getClass().getSimpleName()));
    }
  }

  public void releaseService() {
    super.releaseService();
    stopAllTimers();
  }

  public void stopAllTimers() {
    for (Timer timer : encoders.values()) {
      timer.stop();
    }
  }

  /**
   * Attach to a ServoControl, this will subscribe to moveTo commands and
   * generate ServoData events to be consumed by EncoderListeners. Sequences of
   * events will be generated based on speed and calculated distance and time.
   * 
   * This starts the time counter
   * 
   * @param sc
   * @throws Exception
   */
  public void attach(ServoControl sc) throws Exception {
    if (encoders.containsKey(sc.getName())) {
      log.info("{}.attach({}) already attached", getName(), sc.getName());
      return;
    }
    subscribe(sc.getName(), "publishMoveTo");
    encoders.put(sc.getName(), new Timer(sc));
    sc.attach(this);
  }

  @Override // from EncoderController
  public EncoderData publishEncoderData(EncoderData data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void disable() {
    // TODO Auto-generated method stub

  }

  @Override
  public void enable() {
    // TODO Auto-generated method stub

  }

  @Override
  public Boolean isEnabled() {
    // TODO Auto-generated method stub
    return null;
  }
  
  public void calculateTrajectory(String name, double inBeginPos, double inTargetPos, Double inSpeed) {
    Timer t = encoders.get(name);
    t.calculateTrajectory(inBeginPos, inTargetPos, inSpeed);
  }

  /**
   * published ServoControl.moveTo command - find the distance the time and the
   * divisions
   * 
   * FIXME - does publishMoveTo strategically need the whole ServoControl or
   * just move ? since the initial attach a full ServoControl is sent as
   * reference ?
   * 
   * if not currently running - needs to handle request if currently running -
   * it needs to cancel and handle the new request
   * 
   */
  @Override
  public void onMoveTo(ServoControl sc) {
    Timer timer = encoders.get(sc.getName());
    if (timer == null) {
      log.error("unknown timer {}", sc.getName());
    }
    synchronized (timer) { // TODO - change to "this" when not a service
      // this starts the time encoder thread waiting for move commands
      // to start the process of processing a "planned move" sequence
      timer.notifyAll();
    }
  }
}