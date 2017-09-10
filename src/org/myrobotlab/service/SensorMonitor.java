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
 * TODO - generalize all tracing to a static image package (share with Arduino)
 * 
 * 
 * */

package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.Trigger;
import org.slf4j.Logger;

/**
 * 
 * SensorMonitor - he SensorMonitor takes a variety of input data and displays
 * it to a user interface. It also has the capability of adding alerts. An alert
 * would be triggered if a sensor goes above or below some threshold.
 *
 */
public class SensorMonitor extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(SensorMonitor.class.getCanonicalName());

  public HashMap<String, Trigger> triggers = new HashMap<String, Trigger>();

  public HashMap<String, Trigger> triggers_nameIndex = new HashMap<String, Trigger>();
  public HashMap<String, Pin> lastValue = new HashMap<String, Pin>();

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.init(Level.DEBUG);

    try {

      SensorMonitor sm = new SensorMonitor("sensors");
      sm.startService();

      Runtime.createAndStart("arduino", "Arduino");
      Runtime.createAndStart("gui", "SwingGui");

      /*
       * 
       * Random rand = new Random(); for (int i = 0; i < 10000; ++i) { Message
       * msg = new Message(); msg.name="sensors"; msg.sender="SEAR";
       * msg.method="input"; Float[] gps = new Float[]{rand.nextFloat()*200,
       * rand.nextFloat()*200, rand.nextFloat()*200}; msg.data = new
       * Object[]{gps}; //msg.data = new Object[]{rand.nextFloat()*200};
       * sm.in(msg); }
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  final static public String makeKey(Pin pinData) {
    return makeKey(pinData.source, pinData.pin);
  }

  final static public String makeKey(String source, Integer pin) {
    return String.format("%s_%d", source, pin);
  }

  public SensorMonitor(String n) {
    super(n);
  }

  /*
   * publishing point to add trace data to listeners (like the gui)
   */
  public Pin addTraceData(Pin pinData) {
    return pinData;
  }

  public final void addTrigger(String source, String name, int min, int max, int type, int delay, int targetPin) {
    Trigger pa = new Trigger(name, min, max, type, delay, targetPin);
    triggers.put(makeKey(source, targetPin), pa);
    triggers_nameIndex.put(name, pa);
  }

  // sensorInput - an input point for sensor info

  public final void addTrigger(Trigger trigger) {
    if (trigger.pinData.source == null) {
      log.error("addTrigger adding trigger with no source controller - will be based on pin only ! " + trigger.pinData.pin);
    }
    triggers.put(makeKey(trigger.pinData), trigger);
    triggers_nameIndex.put(trigger.name, trigger);
  }

  public int getLastValue(String source, Integer pin) {
    String key = makeKey(source, pin);
    if (lastValue.containsKey(key)) {
      return lastValue.get(key).value;
    }
    log.error("getLastValue for pin " + key + " does not exist");
    return -1;
  }

  @Override
  public boolean preProcessHook(Message m) // FIXME - WTF???
  {
    if (m.method.equals("input")) {
      if (m.data.length != 1) {
        log.error("where's my data");
        return false;
      }

      Object data = m.data[0];
      if (data instanceof Float) {
        Pin pinData = new Pin(0, 0, ((Float) data).intValue(), m.sender);
        sensorInput(pinData);
      }

      return false;
    }
    return true;
  }

  public Trigger publishPinTrigger(Trigger trigger) {
    return trigger;
  }

  public String publishPinTriggerText(Trigger trigger) {
    return trigger.name;
  }

  // output
  public Pin publishSensorData(Pin pinData) {
    // TODO - wrap with more info if possible
    return pinData;
  }

  public void removeTrigger(String name) {
    if (triggers_nameIndex.containsKey(name)) {
      triggers.remove(name);
      triggers_nameIndex.remove(name);
    } else {
      log.error("removeTrigger " + name + " not found");
    }

  }

  /*
   * sensorInput is the destination of sensor data all types will funnel into a
   * pinData type - this is used to standardize and simplify the display.
   * Additionally, the source can be attached so that trace lines can be
   * identified
   * 
   */
  public void sensorInput(Pin pinData) {
    String key = makeKey(pinData);

    if (triggers.containsKey(key)) {
      Trigger trigger = triggers.get(key);

      if (trigger.threshold < pinData.value) {
        trigger.pinData = pinData;
        invoke("publishPinTrigger", trigger);
        invoke("publishPinTriggerText", trigger);// FIXME - deprecate -
        // silly
        triggers.remove(key);
      }
    }

    if (!lastValue.containsKey(key)) {
      lastValue.put(key, pinData);
    }

    lastValue.get(key).value = pinData.value;

    invoke("publishSensorData", pinData);

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

    ServiceType meta = new ServiceType(SensorMonitor.class.getCanonicalName());
    meta.addDescription("sensor monitor - capable of displaying sensor information in a crude oscilliscope fasion");
    meta.addCategory("sensor", "display");

    return meta;
  }

}
