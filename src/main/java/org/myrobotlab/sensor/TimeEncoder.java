package org.myrobotlab.sensor;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.EncoderControl;
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
  Double estimatedPos = null;

  // current time
  long now;

  // name of encoder data source
  String name;

  boolean autoProcess = true;

  boolean enabled = true;

  static class Positions implements Runnable {

    static Positions instance = null;
    Map<String, Double> positions = null;
    transient private Thread worker;
    transient boolean running = false;
    String filename = null;
    // reference counter - when 0 shuts thread down
    int refCount;

    public Positions() {
      // load previous positions
      String positionsDir = Service.getDataDir(Servo.class.getSimpleName());
      filename = positionsDir + File.separator + "positions.json";
      
      if (positions == null) {
        Map<String, Double> savedPositions = null;
        try {
          String json = FileIO.toString(filename);
          if (json != null) {
            savedPositions = CodecUtils.fromJson(json, ConcurrentHashMap.class);
          }
        } catch (Exception e) {
        }
        if (savedPositions != null) {
          positions = savedPositions;
        } else {
          positions = new ConcurrentHashMap<>();
        }
      }
    }

    @Override
    public void run() {

      running = true;

      while (running) {
        try {
          Thread.sleep(2000);
          log.debug("saving {} positions of {} servos", filename, positions.size());
          FileIO.toFile(filename, CodecUtils.toJson(positions).getBytes());
        } catch (Exception e) {
          log.error("could not save servo positions", e);
        }
      } // while (running)

      worker = null;
    }

    synchronized public void release() {
      --refCount;
      if (refCount == 0) {
        // no one is using time encoders....
        // shutting down
        running = false;
      }
    }

    synchronized public void start() {
      ++refCount;
      if (worker == null) {
        worker = new Thread(this, "TimeEncoder.Positions");
        worker.start();
      }
    }

    public void setPosition(String name, double pos) {
      positions.put(name, pos);
    }

    public static Positions getInstance() {
      if (instance == null) {
        instance = new Positions();
      }
      return instance;
    }

    public Double getPosition(String name) {
      if (positions.containsKey(name)) {
        return positions.get(name);
      }
      return null;
    }
  }

  Positions positions = null;

  public TimeEncoder(ServoControl servo) {
    this.servo = servo;
    this.name = servo.getName();
    positions = Positions.getInstance();
    Double p = positions.getPosition(servo.getName());
    if (p != null) {
      beginPos = targetPos = estimatedPos = p;
    }
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
    
    long lengthOfMoveMs = ((endMoveTs - beginMoveTs) < 0 )?0:endMoveTs - beginMoveTs;
    
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
          this.wait();
        }

        if (speedDegreesPerMs == 0) {
          // FIXME may need some adjustment in this - should a stop event with 0
          // pos change be sent ?
          log.info("speed is 0 - not moving");
          continue;
        }

        while (now < endMoveTs && isRunning) {

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

          positions.setPosition(name, estimatedPos);
          servo.onEncoderData(d);
          Service.sleep(sampleIntervalMs);
        }

        // when we are leaving - its a "finished move"
        // log.info("finished moved");
        EncoderData d = new EncoderData(name, null, estimatedPos);
        servo.onEncoderData(d);
        positions.setPosition(name, estimatedPos);
      }
    } catch (InterruptedException e) {
      log.info("stopping TimeEncoder Timer ...");
    }
    myThread = null;
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
    positions.release();
  }

  @Override
  public void enable() {
    positions.start();
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
    positions.setPosition(name, estimatedPos);
  }

}