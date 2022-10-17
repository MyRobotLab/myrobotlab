package org.myrobotlab.sensor;

import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.Broadcaster;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderController;
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
 *         FIXME - THIS SHOULD BE A SERVICE - similar to PID where it can manage
 *         multiple calculations/trajectories/encoding for many other services
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
  int sampleIntervalMs = 200;

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
  Double estimatedPos = null;

  // current time
  long now;

  // name of encoder data source
  String name;

  boolean autoProcess = true;

  boolean enabled = true;

  protected boolean stopMove = false;

  public TimeEncoder(ServoControl servo) {
    this.servo = servo;
    this.name = servo.getName();
    enable();
  }

  // TODO - cool this works deprecate other
  public long calculateTrajectory(double inBeginPos, double inTargetPos, Double inSpeed) {
    // find current distance - // make a plan ...
    beginPos = inBeginPos;
    targetPos = inTargetPos;
    distance = targetPos - beginPos;
    // always positive ? :P FIXME units for ms
    tspeed = (inSpeed == null) ? defaultMaxSpeedDegreesPerMs : inSpeed / 1000;
    speedDegreesPerMs = (beginPos > targetPos) ? -1 * tspeed : tspeed;

    moveTimeMs = Math.abs(distance / speedDegreesPerMs);
    beginMoveTs = System.currentTimeMillis();
    endMoveTs = beginMoveTs + (long) moveTimeMs;

    // log.debug("{}", this);

    estimatedPos = inBeginPos;

    // while not done ... do
    // leave if timets > endMoveTs or if canceled with new move
    // while()
    now = beginMoveTs;

    if (autoProcess) { // vs buffer ?
      processTrajectory(name);
    }

    long lengthOfMoveMs = ((endMoveTs - beginMoveTs) < 0) ? 0 : endMoveTs - beginMoveTs;

    return lengthOfMoveMs;
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
          stopMove = false;
          this.wait();
          stopMove = false;
        }

        if (speedDegreesPerMs == 0) {
          // FIXME may need some adjustment in this - should a stop event with 0
          // pos change be sent ?
          log.info("speed is 0 - not moving");
          continue;
        }

        boolean started = true;

        while (now < endMoveTs && isRunning) {

          if (stopMove) {
            endMoveTs = now;
            break;
          }

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
          // - Kwatters - SHOULD PROBABLY BE -> EncoderData(name, null,
          // targetPos, estimatedPos) !!!
          EncoderData d = new EncoderData(name, null, estimatedPos, estimatedPos);

          if (started) {
            // ((Broadcaster)servo).broadcast("publishedServoStopped",
            // ServoStatus.SERVO_STOPPED, estimatedPos);
            ((Broadcaster) servo).broadcast("publishServoStarted", servo.getName(), estimatedPos);
            started = false;
          }
          servo.onEncoderData(d);// FIXME !! - broadcast this
          Service.sleep(sampleIntervalMs);
        }

        // when we are leaving - its a "finished move"
        // log.info("finished moved");
        EncoderData d = new EncoderData(name, null, estimatedPos, estimatedPos);
        servo.onEncoderData(d);
        // ((Broadcaster)servo).broadcast("publishedServoStopped",
        // ServoStatus.SERVO_STOPPED, estimatedPos);
        // FYI - broadcast by-passes queues, but can publish based on notify
        // entries
        ((Broadcaster) servo).broadcast("publishServoStopped", servo.getName(), estimatedPos);
      }
    } catch (InterruptedException e) {
      log.info("stopping TimeEncoder Timer ...");
    }
    myThread = null;
  }

  @Override
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

  @Override
  public Double getPos() {
    return estimatedPos;
  }

  public void setPos(Double pos) {
    beginPos = targetPos = estimatedPos = pos;
  }

  @Override
  public boolean hasInterface(Class<?> class1) {
    Class<?>[] faces = TimeEncoder.class.getInterfaces();
    for (Class<?> c : faces) {
      if (c.equals(class1)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasInterface(String interfaze) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isType(Class<?> clazz) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isType(String clazz) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void addListener(String localTopic, String otherService, String callback) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addListener(String localTopic, String otherService) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeListener(String localTopic, String otherService, String callback) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeListener(String localTopic, String otherService) {
    // TODO Auto-generated method stub

  }

  @Override
  public Set<String> getAttached(String publishingPoint) {
    // TODO Auto-generated method stub
    return null;
  }

  public void stopMove() {
    stopMove = true;
  }

  @Override
  public void attachEncoderController(EncoderController controller) {
    // NoOp, the TimeEncoder doesn't need a controller.
  }
}