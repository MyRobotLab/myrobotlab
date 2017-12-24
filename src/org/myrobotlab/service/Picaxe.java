/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * 
 * PICAXE - this service is for the PICAXE microcontroller.
 * 
 * http://www.picaxe.com/
 * 
 * Currently supports: Arduino Duemilanove -
 * http://arduino.cc/en/Main/ArduinoBoardDuemilanove
 * 
 * - Find Arduino Message set - DigitalWrite (pin, data?) - ArduinoProgram
 * HashMap&lt;Key, Program&gt; - loadProgram (Key) - key - default key &amp; program
 * 
 * References: http://www.arduino.cc/playground/Main/RotaryEncoders
 * 
 */
public class Picaxe extends Service // implements SerialPortEventListener,
// DigitalIO, AnalogIO, ServoController
{

  public class PICAXEThread implements Runnable {
    public Thread thread = null;
    public boolean isRunning = true;

    PICAXEThread() {
      thread = new Thread(this, getName() + "_ticking_thread");
      thread.start();
    }

    @Override
    public void run() {
      try {
        while (isRunning == true) {
          if (pulseDataType == PulseDataType.increment) {
            invoke("pulse", pulseDataInteger);
            ++pulseDataInteger;
          } else if (pulseDataType == PulseDataType.integer) {
            invoke("pulse", pulseDataInteger);
          } else if (pulseDataType == PulseDataType.none) {
            invoke("pulse");
          } else if (pulseDataType == PulseDataType.string) {
            invoke("pulse", pulseDataString);
          }

          Thread.sleep(interval);
        }
      } catch (InterruptedException e) {
        log.info("PICAXEThread interrupt");
        isRunning = false;
      }
    }
  }

  // types
  public enum PulseDataType {
    none, integer, increment, string
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Picaxe.class.getCanonicalName());
  // fields
  public int interval = 1000;
  public PulseDataType pulseDataType = PulseDataType.none;
  public String pulseDataString = null;

  public int pulseDataInteger;;

  public transient PICAXEThread myPICAXE = null;

  public static void main(String[] args) throws Exception {
    LoggingFactory.init(Level.DEBUG);

    // RemoteAdapter remote = new RemoteAdapter("remote");
    // remote.startService();
    // test

    Picaxe PICAXE = new Picaxe("PICAXE");
    PICAXE.startService();

    RemoteAdapter remote = new RemoteAdapter("remote");
    remote.startService();

    // PICAXE.addListener("pulse", "log", "log", Integer.class);

    // SwingGui gui = new SwingGui("gui");
    // gui.startService();
    //

    /*
     * FileOutputStream fos = null; ObjectOutputStream out = null; try {
     * 
     * fos = new FileOutputStream("test.backup"); out = new
     * ObjectOutputStream(fos); out.writeObject(remote); out.writeObject(log);
     * out.writeObject(PICAXE); out.writeObject(gui); out.close();
     * 
     * 
     * log.startService();
     * 
     * PICAXE.startService(); PICAXE.startPICAXE();
     * 
     * gui.startService();
     * 
     * 
     * } catch (Exception e) { log.error(e.getMessage());
     * log.error(stackToString(e)); }
     */

  }

  public Picaxe(String n) {
    super(n);
  }

  public Picaxe getState() {
    return this;
  }

  // new state functions begin --------------------------
  @Override
  public Picaxe publishState() {
    return this;
  }

  public void pulse() {
  }

  public Integer pulse(Integer count) {
    log.info("pulse " + count);
    return count;
  }

  public String pulse(String d) {
    return d;
  }

  public void setInterval(Integer milliseconds) {
    interval = milliseconds;
  }

  public Integer setPulseDataInteger(Integer s) {
    pulseDataInteger = s;
    return s;
  }

  public String setPulseDataString(String s) {
    pulseDataString = s;
    return s;
  }

  // TODO - how
  public void setPulseDataType(PulseDataType t) {
    pulseDataType = t;
  }

  // TODO - reflectively do it in Service? !?
  // No - the overhead of a Service warrants a data only proxy - so to
  // a single container class "PICAXEData data = new PICAXEData()" could allow
  // easy maintenance and extensibility - possibly even reflective sync if
  // names are maintained
  public Picaxe setState(Picaxe o) {
    this.interval = o.interval;
    this.pulseDataInteger = o.pulseDataInteger;
    this.pulseDataString = o.pulseDataString;
    // this.myPICAXE = o.myPICAXE;
    this.pulseDataType = o.pulseDataType;
    return o;
  }

  // new state functions end ----------------------------

  public void setType(PulseDataType t) {
    pulseDataType = t;
  }

  // TODO - enum pretty unsuccessful as
  // type does not make it through Action
  public void setType(String t) {
    if (t.compareTo("none") == 0) {
      pulseDataType = PulseDataType.none;
    } else if (t.compareTo("increment") == 0) {
      pulseDataType = PulseDataType.increment;

    } else if (t.compareTo("string") == 0) {
      pulseDataType = PulseDataType.string;

    } else if (t.compareTo("integer") == 0) {
      pulseDataType = PulseDataType.integer;

    } else {
      log.error("unknown type " + t);
    }
  }

  public void startPICAXE() {
    if (myPICAXE == null) {
      myPICAXE = new PICAXEThread();
    }
  }

  public void stopPICAXE() {
    if (myPICAXE != null) {
      log.info("stopping " + getName() + " myPICAXE");
      myPICAXE.isRunning = false;
      myPICAXE.thread.interrupt();
      myPICAXE.thread = null;
      myPICAXE = null;
    }
  }

  @Override
  public void stopService() {
    stopPICAXE();
    super.stopService();
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

    ServiceType meta = new ServiceType(Picaxe.class.getCanonicalName());
    meta.addDescription("Picaxe microcontroller");
    meta.addCategory("microcontroller");
    meta.addPeer("serial", "Serial", "serial service");
    meta.setAvailable(false);
    return meta;
  }

}
