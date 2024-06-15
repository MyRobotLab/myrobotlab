package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.VirtualArduino;
import org.myrobotlab.virtual.VirtualServo;
import org.slf4j.Logger;

/**
 * copy pasted from MrlServo.h
 *
 */
public class MrlServo extends Device implements VirtualServo {

  public final static Logger log = LoggerFactory.getLogger(MrlServo.class);

  public static final int SERVO_EVENT_STARTED = 0;
  public static final int SERVO_EVENT_STOPPED = 1;

  public int pin;
  public boolean isMoving;
  boolean isSweeping;
  public int targetPosUs;
  public float currentPosUs;
  public int minUs;
  public int maxUs;
  public long lastUpdate;
  public int velocity; // in deg/sec | velocity < 0 == no speed control
  public int sweepStep;
  public int acceleration;
  public long moveStart;
  public boolean enabled = false;

  public MrlServo(int deviceId, VirtualArduino virtual) {
    super(deviceId, Msg.DEVICE_TYPE_SERVO, virtual);
    isMoving = false;
    isSweeping = false;
    lastUpdate = 0;
    currentPosUs = 0.0f;
    targetPosUs = 0;
    velocity = -1;
    acceleration = -1;
    moveStart = 0;
  }

  /*
   * MrlServo::~MrlServo() { if (servo){ servo->detach(); delete servo; } }
   */

  // this method "may" be called with a pin or pin & pos depending on
  // config size
  boolean attach(int pin, int initPosUs, int initVelocity, String name) {
    // msg->publishDebug("MrlServo.deviceAttach !");
    attach(pin);
    writeMicroseconds(initPosUs);
    velocity = initVelocity;
    return true;
  }

  // This method is equivalent to Arduino's Servo.attach(pin) - (no pos)
  public void attachPin(int pin) {
    log.info("servo id {}->attachPin({})", id, pin);
    attach(pin);
  }

  public void detachPin() {
    log.info("servo id {}->detachPin()", id);
    detach();
  }

  // FIXME - what happened to events ?
  @Override
  public void update() {
    // it may have an imprecision of +- 1 due to the conversion of currentPosUs
    // to int
    if (isMoving) {
      if ((int) currentPosUs != targetPosUs) {
        // msg->publishDebug("UPDATE");
        long deltaTime = millis() - lastUpdate;
        lastUpdate = millis();
        float _velocity = velocity;
        if (acceleration != -1) {
          _velocity *= acceleration * Math.pow(((float) (millis() - moveStart)) / 1000, 2) / 10;
          // msg->publishDebug(String(_velocity));
          if (_velocity > velocity) {
            _velocity = velocity;
          }
        }
        if (targetPosUs > 500) {
          // target position less than 500 is considered an angle!
          // if the target position is greater than 500 we assume it's
          // microseconds
          // and as a result, our velicity needs to be mapped from degrees to
          // microseconds.
          _velocity = map(_velocity, 0, 180, 544, 2400) - 544;
        }
        float step = _velocity * deltaTime;
        step /= 1000; // for deg/ms;
        if (isSweeping) {
          step = sweepStep;
        }
        if (velocity < 0) {
          // when velocity < 0, it mean full speed ahead
          step = targetPosUs - (int) currentPosUs;
        }
        if (currentPosUs > targetPosUs) {
          // check the direction of the update moving forward or backwards
          step *= -1;
        }
        int previousCurrentPosUs = (int) currentPosUs;
        currentPosUs += step;
        if ((step > 0.0 && (int) currentPosUs > targetPosUs) || (step < 0.0 && (int) currentPosUs < targetPosUs)) {
          // don't over shoot the target.
          currentPosUs = targetPosUs;
        }
        // There was a change in the currentPosition
        if (previousCurrentPosUs != (int) currentPosUs) {
          writeMicroseconds((int) currentPosUs);
          // publishServoEvent(SERVO_EVENT_STARTED);
          // if we're not sweeping and we reached the target position., then we
          // are stopped here.
          if (!isSweeping && ((int) currentPosUs == targetPosUs)) {
            publishServoEvent(SERVO_EVENT_STOPPED);
            isMoving = false;
          }
        }
      } else {
        // current position is target position.
        // if we're sweeping, flip our target position
        if (isSweeping) {
          if (targetPosUs == minUs) {
            targetPosUs = maxUs;
          } else {
            targetPosUs = minUs;
          }
          sweepStep *= -1;
        } else {
          // if we're not sweeping, we have arrived at our final destination.
          isMoving = false;
          publishServoEvent(SERVO_EVENT_STOPPED);
        }
      }
    }
  }

  public void moveToMicroseconds(int posUs) {
    targetPosUs = posUs;
    isMoving = true;
    lastUpdate = millis();
    moveStart = lastUpdate;
    publishServoEvent(SERVO_EVENT_STARTED);
  }

  void startSweep(int minUs, int maxUs, int step) {
    this.minUs = minUs;
    this.maxUs = maxUs;
    sweepStep = step;
    targetPosUs = maxUs;
    isMoving = true;
    isSweeping = true;
    publishServoEvent(SERVO_EVENT_STARTED);
  }

  void stop() {
    isMoving = false;
    isSweeping = false;
    publishServoEvent(SERVO_EVENT_STOPPED);
  }

  void stopSweep() {
    isMoving = false;
    isSweeping = false;
    publishServoEvent(SERVO_EVENT_STOPPED);
  }

  void setVelocity(int velocity) {
    this.velocity = velocity;
  }

  void setAcceleration(int acceleration) {
    this.acceleration = acceleration;
  }

  private long millis() {
    return System.currentTimeMillis();
  }

  private void publishServoEvent(int type) {
    msg.publishServoEvent(id, type, (int) currentPosUs, targetPosUs);
  }

  public void servoWriteMicroseconds(int position) {
  }

  private float map(float value, int inMin, int inMax, int outMin, int outMax) {
    return outMin + ((value - inMin) * (outMax - outMin)) / (inMax - outMax);
  }

  @Override
  public String getName() {
    // TODO: give the servo a name
    return null;
  }

  @Override
  public void writeMicroseconds(int posUs) {
    log.info("writeMicroseconds {}", posUs);
    currentPosUs = posUs;
    targetPosUs = posUs;
  }

  @Override
  public void attach(int pin) {
    // System.out.println("mrl.attach");
    enabled = true;
    this.pin = pin;
  }

  @Override
  public void detach() {
    // System.out.println("mrl.detach");
    enabled = false;
  }

};