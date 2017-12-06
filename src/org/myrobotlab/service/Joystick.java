/**
  This controller supports a game pad with two
 analog sticks with axes (x,y) and (z,rz), 12 buttons, a 
 D-Pad acting as a point-of-view (POV) hat, and a 
 single rumbler.

 The sticks are assumed to be absolute and analog, while the 
 hat and buttons are absolute and digital.

 -----
 The sticks and hat data are accessed as compass directions
 (e.g. NW, NORTH). The compass constants are public so they can be 
 used in the rest of the application.

 The buttons values (booleans) can be accessed individually, or 
 together in an array. 

 The rumbler can be switched on/off, and its current status retrieved.
 
 @author Andrew Davison, October 2006, ad@fivedots.coe.psu.ac.th
 
 created by Andrew Davison, appreciated & hacked up by GroG :)
 */

package org.myrobotlab.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.data.JoystickData;
import org.slf4j.Logger;

//import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Rumbler;

/**
 * Joystick - The joystick service supports reading data from buttons and
 * joysticks. It supports many joysticks, though the button mapping may vary
 * from controller to controller. Component is a general descriptor for any form
 * of "Component" from JInput. Since Component is not serializable we need to
 * move the relevant descriptive data to InputDevice and send that information
 * to describe JInput's Components
 * 
 * To Test java -Djava.library.path="./" -cp "./*"
 * net.java.games.input.test.ControllerReadTest
 */
public class Joystick extends Service {

  public final static Logger log = LoggerFactory.getLogger(Joystick.class);
  private static final long serialVersionUID = 1L;

  /**
   * array of "real" hardware non-serializable controls
   */
  transient net.java.games.input.Controller[] hardwareControllers;
  /**
   * current selected controller
   */
  transient net.java.games.input.Controller hardwareController = null;
  /**
   * array of "real" non-serializable hardware hwComponents
   */

  Map<String, Set<MRLListener>> idAndServiceSubscription = new HashMap<String, Set<MRLListener>>();

  transient net.java.games.input.Component[] hardwareComponents; // holds the
  transient Rumbler[] hardwareRumblers;
  transient InputPollingThread pollingThread = null;
  boolean isPolling = false;

  TreeMap<String, Integer> controllerNames = new TreeMap<String, Integer>();

  // FIXME - lame not just last index :P
  int rumblerIdx; // index for the rumbler being used
  boolean rumblerOn = false; // whether rumbler is on or off

  /**
   * non-transient serializable definition
   */
  Map<String, Mapper> mappers = new HashMap<String, Mapper>();
  Map<String, Component> components = null;

  String controller;

  static public class Component implements Serializable, NameProvider {
    private static final long serialVersionUID = 1L;
    public String id;
    public boolean isRelative = false;
    public boolean isAnalog = false;
    public String type;
    public int index;
    public float value = 0;
    String serviceName;

    public Component(String serviceName, int index, net.java.games.input.Component c) {

      this.serviceName = serviceName;
      this.index = index;
      this.isRelative = c.isRelative();
      this.isAnalog = c.isAnalog();
      this.type = c.getIdentifier().getClass().getSimpleName();
      this.id = c.getIdentifier().toString();
    }

    @Override
    public String toString() {
      return String.format("%d %s [%s] relative %b analog %b", index, type, id, isRelative, isAnalog);
    }

    @Override
    public String getName() {
      return serviceName;
    }
  }

  public class InputPollingThread extends Thread {

    public InputPollingThread(String name) {
      super(name);
    }

    public void run() {
      poll();
    }
  }

  public void poll() {

    net.java.games.input.Controller pollingController = null;
    net.java.games.input.Component[] hwComponents = null;
    
    while (isPolling) {
      try {
        
        if (pollingController != hardwareController){
          // the controller was switched !
          /* Get all the axis and buttons */
          pollingController = hardwareController;
          hwComponents = pollingController.getComponents();
          info("found %d hwComponents", hwComponents.length);          
          broadcastState();
        }
        
        if (pollingController == null) {
          error("controller is null - can not poll");
          stopPolling();
        }        

        // get the data
        if (!pollingController.poll()){
          error("failed to poll controller");
          stopPolling();
        }
        

        // iterate through each component and compare last values
        for (int i = 0; i < hwComponents.length; i++) {

          net.java.games.input.Component hwComp = hwComponents[i];
          float input = hwComp.getPollData();
          String id = hwComp.getIdentifier().toString();
          Component component = components.get(id);
          if (component == null) {
            log.error("{} component is not valid", id);
            continue;
          }

          // if delta enough
          if (Math.abs(input - component.value) > 0.0001) {

            if (mappers.containsKey(id)) {
              input = (float) mappers.get(id).calcOutput(input);
            }

            JoystickData data = new JoystickData(id, input);
            invoke("publishJoystickInput", data);

            // filtered by subscribed components
            if (idAndServiceSubscription.containsKey(id)) {
              Set<MRLListener> listeners = idAndServiceSubscription.get(id);
              for (MRLListener listener : listeners) {
                // "publishJoystickData" -> onJoystickData
                send(listener.callbackName, listener.callbackMethod, data);
              }
            }

          } // if (lastValue == null || Math.abs(input - lastValue) >
            // 0.0001)

          component.value = input;
        }

        Thread.sleep(20);
      } catch (Exception e) {
        log.info("leaving {} polling thread leaving", getName());
        pollingThread = null;
      }
    }
  }

  public Joystick(String n) {
    super(n);
  }

  public Map<String, Component> getComponents() {
    components = new HashMap<String, Component>();
    if (hardwareController == null) {
      info("getComponents no controller set");
      return components;
    }

    hardwareComponents = hardwareController.getComponents();
    if (hardwareComponents.length == 0) {
      error("getComponents no Components found");
      return components;
    }

    info("Num. Components: " + hardwareComponents.length);
    for (int i = 0; i < hardwareComponents.length; i++) {
      net.java.games.input.Component c = hardwareComponents[i];
      String id = c.getIdentifier().toString();
      Component component = new Component(getName(), i, c);
      log.info("found {}", component);
      components.put(id, component);
    }
    return components;
  }

  public Map<String, Integer> getControllers() {
    log.info(String.format("%s getting controllers", getName()));
    hardwareControllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
    info(String.format("found %d controllers", hardwareControllers.length));
    controllerNames.clear();
    for (int i = 0; i < hardwareControllers.length; i++) {
      log.info(String.format("Found input device: %d %s", i, hardwareControllers[i].getName()));
      controllerNames.put(String.format("%d - %s", i, hardwareControllers[i].getName()), i);
    }
    return controllerNames;
  }

  public boolean isPolling() {
    return isPolling;
  }

  public boolean isRumblerOn() {
    return rumblerOn;
  }

  public void map(String name, float x0, float x1, float y0, float y1) {
    Mapper mapper = new Mapper(x0, x1, y0, y1);
    mappers.put(name, mapper);
  }

  public void addInputListener(Service service) {
    service.subscribe(this.getName(), "publishJoystickInput");
  }

  public void addListener(String serviceName, String id) {
    if (!components.containsKey(id)) {
      error("%s requests subscription to component %s - but %d does not exist", serviceName, id, id);
    }
    Set<MRLListener> listeners = null;
    if (idAndServiceSubscription.containsKey(id)) {
      listeners = idAndServiceSubscription.get(id);
    } else {
      listeners = new HashSet<MRLListener>();
    }
    idAndServiceSubscription.put(id, listeners);
    MRLListener listener = new MRLListener("publishJoystickData", serviceName, "onJoystickData");
    listeners.add(listener);
    // addListener("publishJoystickData", serviceName,
    // CodecUtils.getCallBackName("publishJoystickData"));
  }

  public JoystickData publishJoystickInput(final JoystickData input) {
    log.debug(String.format("publishJoystickInput %s", input));
    return input;
  }

  public boolean setController(int index) {
    log.info(String.format("attaching controller %d", index));

    if (index > -1 && index < hardwareControllers.length) {
      hardwareController = hardwareControllers[index];
      controller = String.format("%d - %s", index, hardwareController.getName());
      getComponents();
      startPolling();
      broadcastState();
      return true;
    }

    controller = null;
    error("setController %d bad index", index);
    return false;
  }

  public boolean setController(String s) {
    if (controllerNames.containsKey(s)) {
      setController(controllerNames.get(s));
      return true;
    }
    error("setController - can't find %s", s);
    return false;
  }

  public void setRumbler(boolean switchOn) {
    if (rumblerIdx != -1) {
      if (switchOn)
        hardwareRumblers[rumblerIdx].rumble(0.8f); // almost full on for
      // last
      // rumbler
      else
        // switch off
        hardwareRumblers[rumblerIdx].rumble(0.0f);
      rumblerOn = switchOn; // record rumbler's new status
    }
  } // end of setRumbler()

  synchronized public void startPolling() {
    log.info(String.format("startPolling - starting new polling thread %s_polling", getName()));
    if (pollingThread != null && isPolling == true) {
      log.warn("already polling, stop polling first");
      return;
    }
    isPolling = true;
    pollingThread = new InputPollingThread(String.format("%s_polling", getName()));
    pollingThread.start();
  }

  synchronized public void stopPolling() {
    isPolling = false;
    pollingThread = null;
  }

  public void startService() {
    super.startService();
    invoke("getControllers");
  }

  public String getController() {
    return controller;
  }
  
  public void releaseService(){
    super.releaseService();
    if (pollingThread != null){
      pollingThread.interrupt();
      isPolling = false;
    }
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

    ServiceType meta = new ServiceType(Joystick.class.getCanonicalName());
    meta.addDescription("service allows interfacing with a keyboard, joystick or gamepad");
    meta.addCategory("control");
    meta.addDependency("net.java.games.jinput", "20120914");
    return meta;
  }

  /*
   * Map<String, Set<RelativePositionControl>> axisConsumers = new
   * HashMap<String, Set<RelativePositionControl>>();
   * 
   * @Override public void subscribeToAxis(RelativePositionControl
   * serviceToControl, String axisName) throws Exception {
   * 
   * if (serviceToControl.isLocal()){ Set<RelativePositionControl> callbacks =
   * null; if (axisConsumers.containsKey(axisName)){ callbacks =
   * axisConsumers.get(axisName); } else { callbacks = new
   * HashSet<RelativePositionControl>(); } callbacks.add(serviceToControl);
   * axisConsumers.put(axisName, callbacks); } else { // FIXME - FINISH !! // i
   * want motor to subscribe to my filtered x axis // subscribe() } }
   */
  public Component getAxis(String name) {
    if (components.containsKey(name)) {
      Component c = components.get(name);
      if (!c.isAnalog) {
        warn("getAxis asking for component %s but that component is not analog");
      }
      return c;
    }
    error("getAxis(%s) not found");
    return null;
  }

  public static void main(String args[]) {
    LoggingFactory.init();
    try {

      Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
      // joy.mapId("x", "rx");
      // joy.map("y", -1, 1, 0, 180);
      Runtime.start("cli", "Cli");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

}
