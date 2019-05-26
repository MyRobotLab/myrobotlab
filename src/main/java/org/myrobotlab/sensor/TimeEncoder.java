package org.myrobotlab.sensor;

import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * TimeEncoder - a universal time encoder used for doing estimations and
 * planning of trajectories and paths. It works on a persistent thread put in a
 * wait state, waiting for requested estimations.
 * 
 * @author GroG
 * 
 *         FIXME controlled clear units (degrees cm per s vs ms .. etc)
 *
 */
public class TimeEncoder implements Runnable, EncoderControl {

  public final static Logger log = LoggerFactory.getLogger(TimeEncoder.class);

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

  public TimeEncoder(ServoControl servo) {
    this.servo = servo;
    enable();
  }

  // TODO - cool this works deprecate other
  public void calculateTrajectory(double inBeginPos, double inTargetPos, Double inSpeed) {
    // find current distance - // make a plan ...
    beginPos = inBeginPos;
    targetPos = inTargetPos;
    distance = targetPos - beginPos;
    // always positive ? :P
    tspeed = (inSpeed == null) ? defaultMaxSpeedDegreesPerMs : inSpeed / 1000; // FIXME
                                                                               // UNITS
                                                                               // !!!!
                                                                               // -
                                                                               // for
                                                                               // ms
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
    synchronized (this) { // TODO - change to "this" when not a service
      // this starts the time encoder thread waiting for move commands
      // to start the process of processing a "planned move" sequence
      this.notifyAll();
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
          // FIXME may need some adjustment in this - should a stop event with 0
          // pos change be sent ?
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
          Service.sleep(sampleIntervalMs);
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

  public String toString() {
    return String.format("@ ts %d starting at position %.1f %s will travel %.1f degrees to position %.1f in %.1f ms ending at %d ts", beginMoveTs, beginPos, name, distance,
        targetPos, moveTimeMs, endMoveTs);
  }

  @Override
  public void attach(Attachable service) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void attach(String serviceName) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void detach(Attachable service) {
    // TODO Auto-generated method stub

  }

  @Override
  public void detach(String serviceName) {
    // TODO Auto-generated method stub

  }

  @Override
  public void detach() {
    // TODO Auto-generated method stub

  }

  @Override
  public Set<String> getAttached() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAttached(String name) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void disable() {
    isRunning = false;
    if (myThread != null) {
      myThread.interrupt();
    }
    myThread = null;
  }

  @Override
  public void enable() {
    if (myThread == null) {
      myThread = new Thread(this, String.format("%s.%s.time-encoder", servo.getName(), getName()));
      myThread.start();
    }
  }

  @Override
  public EncoderData publishEncoderData(EncoderData data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Boolean isEnabled() {
    // TODO Auto-generated method stub
    return null;
  }

}