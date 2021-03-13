package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderController;

public class AbstractPinEncoder extends Service implements EncoderControl {

  private static final long serialVersionUID = 1L;
  public String pin;
  // default 12 bit encoder is 4096 steps of resolution
  public Integer resolution = 4096;
  public Double lastPosition = 0.0;
  public EncoderController controller = null;
  boolean enabled = true;
  // we can track the last update that we've recieved and specify the direction
  // even!
  protected long lastUpdate = 0;
  protected Double velocity = 0.0;

  public AbstractPinEncoder(String n, String id) {
    super(n, id);
  }

  public void attach(EncoderController controller) throws Exception {
    if (this.controller == controller) {
      log.info("{} already attached to controller {}", getName(), controller.getName());
    }
    this.controller = controller;
    controller.attach(this);
    lastUpdate = System.currentTimeMillis();
  }

  public String getPin() {
    return pin;
  }

  public Double publishEncoderAngle(Double angle) {
    log.info("Publish Encoder Angle : {}", angle);
    return angle;
  }

  // This is used to relay the data being broadcast from a controller (such as an arduino)
  public void onEncoderData(EncoderData data) {
    // TODO: maybe the raw pin data from the arduino comes in here instead.. 
    // current timestamp / delta since last update.
    long now = System.currentTimeMillis();
    long delta = now - lastUpdate;
    if (delta > 0) {
      // we can compute velocity since the last update
      // This computes the change in degrees per second that the encoder is
      // currently moving at.
      velocity = (data.angle - this.lastPosition) / delta * 1000.0;
    } else {
      // no position update since the last tick.
      velocity = 0.0;
    }
    // update the previous values
    this.lastPosition = data.angle;
    this.lastUpdate = now;
    // log.info("Encoder Data : {} Angle : {}", data, lastPosition);
    // now that we've updated our state.. we can publish along the data.
    broadcast("publishEncoderData", data);
  }

  public void setZeroPoint() {
    // pass the set zero point command to the controller
    controller.setZeroPoint(this);
  }

  public void setPin(String pin) {
    this.pin = pin;
  }

  public void setPin(Integer address) {
    this.pin = String.format("%d", address);
  }

  @Override
  public void disable() {
    enabled = false;
  }

  @Override
  public void enable() {
    enabled = true;
  }

  @Override
  public Boolean isEnabled() {
    return enabled;
  }

  @Override
  public EncoderData publishEncoderData(EncoderData data) {
    return data;
  }

  @Override
  public Double getPos() {
    return lastPosition;
  }
}
