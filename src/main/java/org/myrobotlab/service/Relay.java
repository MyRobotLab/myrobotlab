// TODO UNIVERSAL CONTROLER
package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Relay extends Service {

  public Arduino arduino;
  public Integer pin;
  public Integer onValue = 0;
  public Integer offValue = 1;

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Relay.class);

  public Relay(String n) {
    super(n);
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Relay.class.getCanonicalName());
    meta.addDescription("Relay used by an arduino");
    meta.setAvailable(true);
    meta.addCategory("home automation");
    return meta;
  }

  public void on() {
    if (arduino.isConnected() && pin != null && onValue != null) {
      arduino.pinMode(pin, Arduino.OUTPUT);
      arduino.digitalWrite(pin, onValue);
      log.info("Relay " + this.getName() + " ON");
    } else {
      log.error(this.getName() + " error");
    }
  }

  public void off() {
    if (arduino.isConnected() && pin != null && offValue != null) {
      arduino.pinMode(pin, Arduino.OUTPUT);
      arduino.digitalWrite(pin, offValue);
      log.info("Relay " + this.getName() + " OFF");
    } else {
      log.error(this.getName() + " error");
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      Runtime.start("relay", "Relay");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
