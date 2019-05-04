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

  public class Timer implements Runnable {
    // String type; // FIXME - make enum ... absolute | relative | other ..
    // Long intervalMs;
    ServoControl servo = null;
    boolean isRunning = false;
    // Object lock = new Object();
    transient Thread myThread = null;

    // default max speed
    // Common servos have operating speeds in the range of 0.05 to 0.2 s/60 degree.
    // going with 60 degrees in 0.12 s 
    // 
    double defaultMaxSpeedDegreesPerMs = 0.5;// degrees/ms 0.5 ?
    int sampleIntervalMs = 5;

    public Timer(ServoControl servo) {
      this.servo = servo;
      myThread = new Thread(this, String.format("%s.%s.time-encoder", servo.getName(), getName()));
      myThread.start();
    }

    @Override
    public void run() {
      try {
        synchronized (this) {
          isRunning = true;
          while (isRunning) {
            // wait for next move ...
            this.wait();

            // find current distance - // make a plan ...
            double beginPos = servo.getPos();
            double targetPos = servo.getTargetPos();
            double distance = servo.getTargetPos() - servo.getPos();
            double tspeed = (servo.getVelocity() == null) ? defaultMaxSpeedDegreesPerMs : servo.getVelocity()/1000; // always positive :P
            double speedDegreesPerMs = (beginPos > targetPos)?-1 * tspeed:tspeed;
            double moveTimeMs = Math.abs(distance / speedDegreesPerMs);
            long beginMoveTs = System.currentTimeMillis();
            long endMoveTs = beginMoveTs + (long) moveTimeMs;

            log.info("@ ts {} starting at {} we are going to travel {} degrees to position {} in {} ms ending at {} ts", beginMoveTs, servo.getPos(), distance,
                servo.getTargetPos(), moveTimeMs, endMoveTs);

            double estimatedPos = servo.getPos();
            double lastPos = estimatedPos;

            // while not done ... do
            // leave if timets > endMoveTs or if canceled with new move
            // while()
            long now = beginMoveTs;

            while (now < endMoveTs) {

              now = System.currentTimeMillis();
              estimatedPos = beginPos + speedDegreesPerMs * (now - beginMoveTs); // speed has +/- direction
              
              // actual degree increments - TODO - option to disable and publish back sub-degrees
              /*
              if (Math.round(lastPos) == Math.round(estimatedPos)) {
                sleep(sampleIntervalMs);
                continue;
              }
              */
              
              if (beginPos < targetPos && estimatedPos > targetPos) {
                estimatedPos = targetPos;
              }
              if (beginPos > targetPos && estimatedPos < targetPos) {
                estimatedPos = targetPos;
              }
              
              log.info(String.format("new pos %.2f", estimatedPos)); 
              EncoderData d = new EncoderData(servo.getName(), null, estimatedPos);
              servo.onEncoderData(d);
              lastPos = estimatedPos;
              
              sleep(sampleIntervalMs);
            }

          }
        }
      } catch (InterruptedException e) {
        log.info("stopping TimeEncoder Timer ...");
      }
    }

    public void stop() {
      isRunning = false;
      myThread.interrupt();
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

  /**
   * published ServoControl.moveTo command - find the distance the time and the
   * divisions
   * 
   * FIXME - does publishMoveTo strategically need the whole ServoControl or
   * just move ? since the initial attach a full ServoControl is sent as
   * reference ?
   */
  @Override
  public void onMoveTo(ServoControl sc) {
    Timer timer = encoders.get(sc.getName());
    if (timer == null) {
      log.error("unknown timer {}", sc.getName());
    }
    synchronized (timer) {
      timer.notifyAll();
    }
  }
}