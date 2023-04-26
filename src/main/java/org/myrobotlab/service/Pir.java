package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.PirConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinListener;
import org.slf4j.Logger;

public class Pir extends Service implements PinListener {

  public final static Logger log = LoggerFactory.getLogger(Pir.class);

  private static final long serialVersionUID = 1L;

  /**
   * yep there are 3 states to a binary sensor true/false .... and unknown - we
   * start with "unknown". true = Sensing movement. false = not sensing
   * movement. null = unknown, either disabled or not polled after enabled.
   */
  Boolean active = null;

  /**
   * The pin to be used as a string. Example "D4" or "A0".
   */
  // String pin;

  transient PinArrayControl pinControl;

  /**
   * Timestamp of the last poll.
   */
  Long lastChangeTs = null;

  boolean attached = false;

  public Pir(String n, String id) {
    super(n, id);
    registerForInterfaceChange(PinArrayControl.class);
  }

  @Deprecated /* use attach(String) or attachPinArrayControl(PinArrayControl) */
  public void attach(PinArrayControl control, String pin) {
    setPin(pin);
    attachPinArrayControl(control);
  }

  @Override
  public void attach(String name) {
    ServiceInterface si = Runtime.getService(name);
    if (si instanceof PinArrayControl) {
      attachPinArrayControl((PinArrayControl) si);
    } else {
      error("do not know how to attach to %s of type %s", name, si.getSimpleName());
    }
  }

  public void attachPinArrayControl(PinArrayControl control) {
    PirConfig c = (PirConfig) config;
    try {
      this.pinControl = control;
      c.controller = control.getName();

      if (c.pin == null) {
        error("pin should be set before attaching");
      }
      pinControl.attach(getName());
      attached = true;
      broadcastState();
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void detach(String name) {
    PirConfig c = (PirConfig) config;
    
    ServiceInterface si = Runtime.getService(name);
    if (si instanceof PinArrayControl && c.pin != null) {
      // FIXME - problem - what if someone else is using this pin ?
      // FIXME - should disable in the context of this service's name
      ((PinArrayControl) si).disablePin(c.pin);
      detachPinArrayControl((PinArrayControl) si);
    } 
    
    active = null;
    c.enable = false;
    attached = false;
    broadcastState();
  }

  public void detachPinArrayControl(PinArrayControl control) {
    PirConfig c = (PirConfig) config;

    try {
      if (control == null) {
        log.info("detaching null");
        return;
      }

      if (c.controller != null) {
        if (!c.controller.equals(control.getName())) {
          log.warn("attempting to detach {} but this pir is attached to {}", control.getName(), c.controller);
          return;
        }
      }

      // FYI - we could detach like this without a reference - good for remote
      // send(controllerName, "detach", getName());
      pinControl.detach(getName());

      this.pinControl = null;
      c.controller = null;

      broadcastState();
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * Disables the sensor preventing it from polling the input pin. As a result
   * of disabling the polling, the current state will be null as it will be an
   * unknown state.
   * 
   */
  public void disable() {
    PirConfig c = (PirConfig) config;

    if (pinControl != null && c.pin != null) {
      pinControl.disablePin(c.pin);
    }

    c.enable = false;
    active = null;
    broadcastState();
  }

  /**
   * Enables polling at the preset poll rate.
   */
  public void enable() {
    PirConfig c = (PirConfig) config;
    enable(c.rate);
  }

  /**
   * Enables polling at the set rate.
   * 
   * @param rateHz
   * 
   */
  public void enable(int rateHz) {
    PirConfig c = (PirConfig) config;

    if (pinControl == null) {
      error("pin control not set");
      return;
    }

    if (c.pin == null) {
      error("pin not set");
      return;
    }

    if (rateHz < 1) {
      error("invalid poll rate - default is 1 Hz valid value is > 0");
      return;
    }

    c.rate = rateHz;
    pinControl.enablePin(c.pin, rateHz);
    c.enable = true;
    broadcastState();
  }

  @Override
  public String getPin() {
    PirConfig c = (PirConfig) config;
    return c.pin;
  }

  /**
   * returns the current poll rate in Hz.
   * 
   * @return Hz
   */
  public int getRate() {
    PirConfig c = (PirConfig) config;
    return c.rate;
  }

  /**
   * Returns the current state of the input.
   * 
   * @return true = Sensor is detecting movement. false = Sensor is not
   *         detecting movement. null = Unknown state, the sensor may be disable
   *         or has not yet polled.
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Returns the current Enable state.
   * 
   * @return true = Enabled. false = Disabled.
   */
  public boolean isEnabled() {
    PirConfig c = (PirConfig) config;
    return c.enable;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    PirConfig config = (PirConfig) super.apply(c);

    if (config.enable) {
      enable(config.rate);
    } else {
      disable();
    }

    return c;
  }

  @Override
  public void onPin(PinData pindata) {

    log.debug("onPin {}", pindata);

    boolean sense = (pindata.value != 0);

    // sparse publishing only on state change
    if (active == null || active != sense) {
      // state change
      invoke("publishSense", sense);
      active = sense;
      if (active) {
        invoke("publishPirOn");
      } else {
        invoke("publishPirOff");
      }
      lastChangeTs = System.currentTimeMillis();
    }
  }

  public Boolean publishSense(Boolean b) {
    return b;
  }

  public void publishPirOn() {
    log.info("publishPirOn");
  }

  public void publishPirOff() {
    log.info("publishPirOff");
  }

  /**
   * Sets the pin to use for the Input of the PIR service.
   * 
   * @param pin
   *          A string representing the pin name. example "D4" or "A0".
   */
  @Override
  public void setPin(String pin) {
    PirConfig c = (PirConfig) config;
    c.pin = pin;
  }

  @Deprecated /* use attach(String) */
  public void setPinArrayControl(PinArrayControl pinControl) {
    PirConfig c = (PirConfig) config;
    this.pinControl = pinControl;
    c.controller = pinControl.getName();
  }

  /**
   * Sets the polling rate in Hz.
   * 
   * @param rateHz
   */
  public void setRate(int rateHz) {
    if (rateHz < 1) {
      error("invalid poll rate - default is 1 Hz valid value is > 0");
      return;
    }
    PirConfig c = (PirConfig) config;
    c.rate = rateHz;
  }

  /**
   * The time stamp of the last input poll.
   * 
   * @return Long representing the timestamp.
   */
  public Long getLastChangeTs() {
    return lastChangeTs;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init("info");

      Pir pir = (Pir) Runtime.start("pir", "Pir");
      pir.setPin("D6");

      Runtime.start("webgui", "WebGui");
      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      mega.connect("/dev/ttyACM2");

      boolean done = true;
      if (done) {
        return;
      }

      mega.attach(pir);

      // Runtime.setAllVirtual(true);

      mega.connect("/dev/ttyACM0");
      pir.setPin("D23");
      pir.attach(mega);
      pir.enable();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
