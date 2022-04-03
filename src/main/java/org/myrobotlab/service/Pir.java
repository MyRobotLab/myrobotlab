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

  String controllerName;

  /**
   * yep there are 3 states to a binary sensor true/false .... and unknown - we
   * start with "unknown"
   */
  Boolean active = null;

  boolean isEnabled = false;

  String pin;

  transient PinArrayControl pinControl;

  int rateHz = 1;

  Long lastChangeTs = null;

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
    try {
      if (this.pinControl != null) {
        info("already attached detach first");
        return;
      }
      this.pinControl = control;
      controllerName = control.getName();

      if (pin == null) {
        error("pin should be set before attaching");
      }
      pinControl.attach(getName());
      broadcastState();
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void detach(String name) {
    ServiceInterface si = Runtime.getService(name);
    if (si instanceof PinArrayControl) {
      // FIXME - problem - what if someone else is using this pin ?
      // FIXME - should disable in the context of this service's name
      ((PinArrayControl) si).disablePin(pin);
      detachPinArrayControl((PinArrayControl) si);
    } else {
      error("do not know how to detach to %s of type %s", name, si.getSimpleName());
    }
    active = false;
    isEnabled = false;
    broadcastState();
  }

  public void detachPinArrayControl(PinArrayControl control) {
    try {
      if (control == null) {
        log.warn("detaching null");
        return;
      }

      if (controllerName != null) {
        if (!controllerName.equals(control.getName())) {
          log.warn("attempting to detach {} but this pir is attached to {}", control.getName(), controllerName);
          return;
        }
      }

      // FYI - we could detach like this without a reference - good for remote
      // send(controllerName, "detach", getName());
      pinControl.detach(getName());

      this.pinControl = null;
      controllerName = null;

      broadcastState();
    } catch (Exception e) {
      error(e);
    }
  }

  public void disable() {
    if (pinControl == null) {
      error("pin control not set");
      return;
    }

    if (pin == null) {
      error("pin not set");
      return;
    }

    // FixMe - sendTo(pincontrolName,"disablePin", pin)
    pinControl.disablePin(pin);
    isEnabled = false;
    active = false;
    broadcastState();
  }

  public void enable() {
    enable(rateHz);
  }

  public void enable(int pollBySecond) {
    if (pinControl == null) {
      error("pin control not set");
      return;
    }

    if (pin == null) {
      error("pin not set");
      return;
    }

    pinControl.enablePin(pin, pollBySecond);
    isEnabled = true;
    broadcastState();
  }

  @Override
  public PirConfig getConfig() {

    PirConfig config = new PirConfig();

    config.controller = controllerName;
    config.pin = pin;
    config.enable = isEnabled;
    config.rate = rateHz;

    return config;
  }

  @Override
  public String getPin() {
    return pin;
  }

  public int getRate() {
    return rateHz;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public ServiceConfig apply(ServiceConfig c) {
    PirConfig config = (PirConfig) c;

    if (config.pin != null)
      setPin(config.pin);

    if (config.rate != null)
      setRate(config.rate);

    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch (Exception e) {
        error(e);
      }
    }

    if (config.enable) {
      enable();
    } else {
      disable();
    }

    return c;
  }

  @Override
  public void onPin(PinData pindata) {

    log.info("onPin {}", pindata);

    boolean sense = (pindata.value != 0);

    // sparse publishing only on state change
    if (active == null) {
      invoke("publishSense", sense);
      active = sense;
    } else if (active != sense) {
      // state change
      invoke("publishSense", sense);
      active = sense;
      lastChangeTs = System.currentTimeMillis();
    }
  }

  public Boolean publishSense(Boolean b) {
    return b;
  }

  public void setPin(String pin) {
    this.pin = pin;
  }

  @Deprecated /* use attach(String) */
  public void setPinArrayControl(PinArrayControl pinControl) {
    this.pinControl = pinControl;
    controllerName = pinControl.getName();
  }

  public void setRate(int rateHz) {
    this.rateHz = rateHz;
  }
  
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
