package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.TimeoutException;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.PirConfig;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.slf4j.Logger;

public class Pir extends Service<PirConfig> implements PinListener {

  public final static Logger log = LoggerFactory.getLogger(Pir.class);

  private static final long serialVersionUID = 1L;

  /**
   * yep there are 3 states to a binary sensor true/false .... and unknown - we
   * start with "unknown". true = Sensing movement. false = not sensing
   * movement. null = unknown, either disabled or not polled after enabled.
   */
  Boolean active = null;

  /**
   * Timestamp of the last poll.
   */
  Long lastChangeTs = null;

  protected boolean isAttached = false;

  public Pir(String n, String id) {
    super(n, id);
  }

  @Deprecated /* use attach(String) or attachPinArrayControl(PinArrayControl) */
  public void attach(PinArrayControl control, String pin) {
    setPin(pin);
    attachPinArrayControl(control.getName());
  }

  @Override
  public void attach(String name) {
    attachPinArrayControl(name);
  }

  public void setPinArrayControl(String control) {
    config.controller = control;
  }

  public void attachPinArrayControl(String control) {

    if (control == null) {
      error("controller cannot be null");
      return;
    }

    if (config.pin == null) {
      error("pin should be set before attaching");
      return;
    }

    config.controller = CodecUtils.getShortName(control);

    // fire and forget
    send(config.controller, "attach", getName());
    // assume worky
    isAttached = true;
    
    // enable if configured
    if (config.enable) {
        send(config.controller, "enablePin", config.pin, config.rate);
    }
    
    broadcastState();
  }

  @Override
  public void detach(String name) {
    detachPinArrayControl(name);
  }

  /**
   * FIXME - use interface of service names not direct references
   * 
   * @param control
   */
  public void detachPinArrayControl(String control) {

      if (control == null) {
        log.info("detaching null");
        return;
      }

      if (config.controller != null) {
        if (!config.controller.equals(control)) {
          log.warn("attempting to detach {} but this pir is attached to {}", control, config.controller);
          return;
        }
      }

      // disable
      disable();

      send(config.controller, "detach", getName());
      // config.controller = null; left as configuration .. "last controller"

      // detached
      isAttached = false;

      broadcastState();
  }

  /**
   * Disables the sensor preventing it from polling the input pin. As a result
   * of disabling the polling, the current state will be null as it will be an
   * unknown state.
   * 
   */
  public void disable() {

    if (config.controller != null && config.pin != null) {
      send(config.controller, "disablePin", config.pin);
    }

    config.enable = false;
    active = null;
    broadcastState();
  }

  /**
   * Enables polling at the preset poll rate.
   */
  public void enable() {
    enable(config.rate);
  }

  /**
   * Enables polling at the set rate.
   * 
   * @param rateHz
   * 
   */
  public void enable(int rateHz) {

    if (config.controller == null) {
      error("pin control not set");
      return;
    }

    if (config.pin == null) {
      error("pin not set");
      return;
    }

    if (rateHz < 0) {
      error("invalid poll rate - default is 1 Hz valid value is > 0");
      return;
    }

    config.rate = rateHz;
    /* PinArrayControl.enablePin */
    send(config.controller, "enablePin", config.pin, rateHz);
    config.enable = true;
    broadcastState();
  }

  @Override
  public String getPin() {
    return config.pin;
  }

  /**
   * returns the current poll rate in Hz.
   * 
   * @return Hz
   */
  public int getRate() {
    return config.rate;
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
    return config.enable;
  }

  @Override
  public PirConfig apply(PirConfig c) {
    super.apply(c);
    
    if (config.controller != null) {
      attach(config.controller);;
    }

    if (config.enable) {      
      enable(config.rate);
    } else {
      disable();
    }

    return c;
  }
  
  @Override
  public PirConfig getConfig() {
    super.getConfig();
    if (config.controller != null) {
      // it makes sense that the controller should always be local for a PIR
      // but in general this is bad practice on 2 levels
      // 1. in some other context it might make sense not to be local
      // 2. it should just be another listener on ServiceConfig.listener
      config.controller=CodecUtils.getShortName(config.controller);
    }
    return config;
  }

  @Override
  public void onPin(PinData pindata) {
    log.debug("onPin {}", pindata);

    boolean sense = (pindata.value != 0);

    // sparse publishing only on state change 
    if (active == null || active != sense && config.enable) {
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
    log.debug("publishPirOn");
  }

  public void publishPirOff() {
    log.debug("publishPirOff");
  }

  /**
   * Sets the pin to use for the Input of the PIR service.
   * 
   * @param pin
   *          A string representing the pin name. example "D4" or "A0".
   */
  @Override
  public void setPin(String pin) {
    config.pin = pin;
  }

  @Deprecated /* use attach(String) */
  public void setPinArrayControl(PinArrayControl pinControl) {
    config.controller = pinControl.getName();
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
    config.rate = rateHz;
  }

  /**
   * The time stamp of the last input poll.
   * 
   * @return Long representing the timestamp.
   */
  public Long getLastChangeTs() {
    return lastChangeTs;
  }

  /**
   * This returns the pin list of the selected PinArrayControl. This allows
   * dynamic selection of a pin based on a query to a PinArrayControl. It would
   * be advisable that other services manage pins in the same way. Where
   * "selecting" the controller's name, returns the possible list of pins to
   * attach.
   * 
   * @return
   * @throws InterruptedException
   * @throws TimeoutException
   */
  @SuppressWarnings("unchecked")
  public List<PinDefinition> getPinList(String pinArrayControl) {
    List<PinDefinition> pinList = new ArrayList<>();
    try {
      if (pinArrayControl != null) {
        pinList = (List<PinDefinition>) sendBlocking(pinArrayControl, "getPinList");
      }
    } catch (Exception e) {
      error(e);
    }
    return pinList;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init("info");


      // Runtime.start("webgui", "WebGui");
      
      // standard install - develop and debug using config
      Runtime.main(new String[] {"--log-level", "info", "-s", "webgui", "WebGui", "intro", "Intro", "python", "Python"});
          

      boolean done = true;
      if (done) {
        return;
      }

      Pir pir = (Pir) Runtime.start("pir", "Pir");
      pir.setPin("D23");
      
      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      mega.connect("/dev/ttyACM71");

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
