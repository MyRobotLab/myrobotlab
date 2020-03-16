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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.joystick.Component;
import org.myrobotlab.joystick.Controller;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.data.JoystickData;
import org.slf4j.Logger;

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

  List<Controller> controllers;

  /**
   * current selected controller
   */
  Controller hardwareController = null;

  Map<String, Set<MRLListener>> idAndServiceSubscription = new HashMap<String, Set<MRLListener>>();

  List<Component> hardwareComponents; // holds the

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

  public class InputPollingThread extends Thread {

    public InputPollingThread(String name) {
      super(name);
    }

    public void run() {
      poll();
    }
  }

  public void poll() {

    // v net.java.games.input.Controller pollingController = null;
    // v net.java.games.input.Component[] hwComponents = null;

    Controller pollingController = null;
    Component[] hwComponents = null;

    while (isPolling) {
      try {

        if (pollingController != hardwareController) {
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
        if (!pollingController.poll()) {
          error("failed to poll controller");
          stopPolling();
        }

        // iterate through each component and compare last values
        for (int i = 0; i < hwComponents.length; i++) {

          // v net.java.games.input.Component hwComp = hwComponents[i];
          Component hwComp = hwComponents[i];

          String id = hwComp.getIdentifier().toString();
          /*
           * if (id.equals("3")) { log.info("here"); }
           */

          float input = hwComp.getPollData();

          // log.info("", input);

          Component component = components.get(id);
          if (component == null) {
            log.error("{} component is not valid", id);
            continue;
          }

          // if delta enough
          if (Math.abs(input - component.value) > 0.0001) {

            if (mappers.containsKey(id)) {
              input = mappers.get(id).calcOutput((double)input).floatValue();
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

  public Joystick(String n, String id) {
    super(n, id);
    // we will force a system property here to specify the native location for
    // the
    // jinput libraries
    // TODO: this is a hacky work around because for some reason, the jinput
    // natives
    // aren't found from the jinput-platform jar files!!
    String jinputNativePath = new java.io.File(".").getAbsolutePath() + File.separatorChar + "jinput-natives";
    System.getProperties().setProperty("net.java.games.input.librarypath", jinputNativePath);
    String[] controllers = getControllerNames();
    info("found %d controllers %s", controllers.length, Arrays.toString(controllers));
  }

  // FIXME - simply set components e.g. getComponents
  public Map<String, Component> getComponents() {
    components = new HashMap<String, Component>();
    if (hardwareController == null) {
      info("getComponents no controller set");
      return components;
    }

    components = hardwareController.getComponentMap();

    /*
     * hardwareComponents = hardwareController.getComponents(); if
     * (hardwareComponents.length == 0) {
     * error("getComponents no Components found"); return components; }
     * 
     * info("number of components: " + hardwareComponents.length); for (int i =
     * 0; i < hardwareComponents.length; i++) { // v
     * net.java.games.input.Component c = hardwareComponents[i]; Component c =
     * hardwareComponents[i]; String id = c.getIdentifier().toString(); // v
     * Component component = new Component(getName(), i, c); // v
     * log.info("found {}", component); // v components.put(id, component); }
     */
    return components;
  }

  public Map<String, Integer> getControllers() {

    log.info("{} getting controllers", getName());
    // v hardwareControllers =
    // ControllerEnvironment.getDefaultEnvironment().getControllers();
    controllers = getControllerList();

    info(String.format("found %d controllers", controllers.size()));
    controllerNames.clear();
    for (int i = 0; i < controllers.size(); i++) {
      log.info("Found input device: {} {}", i, controllers.get(i).getName());
      controllerNames.put(String.format("%d - %s", i, controllers.get(i).getName()), i);
    }
    return controllerNames;
  }

  // FIXME - clear global
  public List<Controller> getControllerList() {
    List<Controller> controllers = new ArrayList<Controller>();
    net.java.games.input.Controller[] jinputControllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
    for (net.java.games.input.Controller controller : jinputControllers) {
      try {
        log.info("adding hardware controller {}", controller.getName());
        controllers.add(new Controller(getName(), controller));
      } catch (Exception e) {
        log.error("adding new controller threw", e);
      }
    }
    // FIXME - add virtual
    return controllers;
  }

  public String[] getControllerNames() {
    Map<String, Integer> c = getControllers();
    String[] ret = new String[c.size()];
    int i = 0;
    for (String name : c.keySet()) {
      ret[i] = name;
      ++i;
    }
    return ret;
  }

  public boolean isPolling() {
    return isPolling;
  }

  public boolean isRumblerOn() {
    return rumblerOn;
  }

  public void map(String name, double x0, double x1, double y0, double y1) {
    Mapper mapper = new MapperLinear(x0, x1, y0, y1);
    mappers.put(name, mapper);
  }

  public void addInputListener(Service service) {
    service.subscribe(this.getName(), "publishJoystickInput");
  }

  public void attach(String serviceName, String id) {
    if (!components.containsKey(id)) {
      error("%s requests subscription to component %s - but %d does not exist", serviceName, id, id);
      return;
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
    log.debug("publishJoystickInput {}", input);
    return input;
  }

  public boolean setController(int index) {

    if (index > -1 && index < controllers.size()) {
      hardwareController = controllers.get(index);
      info("attaching controller %d-%s", index, hardwareController.getName());
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
    // getControllers();
    // exact match
    if (controllerNames.containsKey(s)) {
      setController(controllerNames.get(s));
      return true;
    }

    // close match
    for (String controllerName : controllerNames.keySet()) {
      if (controllerName.contains(s)) {
        setController(controllerNames.get(controllerName));
        return true;
      }
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
    log.info("startPolling - starting new polling thread {}_polling", getName());
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

  public void releaseService() {
    super.releaseService();
    if (pollingThread != null) {
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
    meta.addCategory("control","telerobotics");
    meta.addDependency("net.java.jinput", "jinput", "2.0.7");
    meta.addDependency("jinput-natives", "jinput-natives", "2.0.7", "zip");
    // meta.addDependency("net.java.jinput", "jinput-platform", "2.0.7");
    // meta.addArtifact("net.java.jinput", "natives-windows");
    // meta.addArtifact("net.java.jinput", "natives-linux");
    // meta.addArtifact("")
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
    if (components == null) {
      error("%s components null - cannot get axis %s", getName(), name);
      return null;
    }
    if (components.containsKey(name)) {
      Component c = components.get(name);
      if (!c.isAnalog) {
        warn("getAxis asking for component %s but that component is not analog");
      }
      return c;
    }
    error("getAxis(%s) not found", name);
    return null;
  }

  public void pressButton(String buttonName) {
    if (components == null) {
      log.error("controller not set");
      return;
    }

    if (components == null) {
      log.error("components are null");
      return;
    }

    if (!components.containsKey(buttonName)) {
      log.error("component {} does not exist", buttonName);
      return;
    }

    Component component = components.get(buttonName);
    component.setVirtualValue(1.0F);
  }

  public void releaseButton(String buttonName) {
    if (components == null) {
      log.error("controller not set");
      return;
    }

    if (components == null) {
      log.error("components are null");
      return;
    }

    if (!components.containsKey(buttonName)) {
      log.error("component {} does not exist", buttonName);
      return;
    }

    Component component = components.get(buttonName);
    component.setVirtualValue(0.0F);
  }

  public void pressReleaseButton(String buttonName) {
    pressButton(buttonName);
    releaseButton(buttonName);
  }

  public Controller cloneController(int index) throws IOException {
    if (controllers == null) {
      log.error("controllers not set");
      return null;
    }

    Controller controller = controllers.get(index);
    String filename = String.format("%s-virtual-%s-%s.json", getName(), controller.getName(), ++index);

    save(controller, filename);

    // FIXME - non-symmetric save and load :(
    String fname = String.format("%s%s%s", FileIO.getCfgDir(), File.separator, filename);

    Controller v = loadVirtualController(fname);
    return v;
  }

  public Controller loadVirtualController(String filename) throws IOException {
    String json = FileIO.toString(filename);
    Controller controller = CodecUtils.fromJson(json, Controller.class);

    if (controller != null) {
      // v.setName(String.format("virtual %s", controller.getName()));
      controller.reIndex(getName());
      // virtualControllers.add(controller);
      controllers.add(controller);
      controllerNames.put(controller.getName(), controllers.size() - 1);
    } else {
      error("could not load virtual controller from %s", filename);
    }

    return controller;
  }

  public static void main(String args[]) {
    LoggingFactory.init();
    LoggingFactory.setLevel("INFO");
    try {

      Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
      Runtime.start("gui", "SwingGui");

      joy.setController(2);

      // TODO - clone all
      // TODO - virtualize all
      joy.cloneController(2);

      joy.pressButton("1");
      joy.pressButton("2");
      joy.pressButton("3");
      joy.pressButton("4");
      joy.releaseButton("4");
      joy.moveTo("rz", 0.3);
      joy.moveTo("rz", 0.35);
      joy.moveTo("rz", 0.4);
      joy.moveTo("rz", 0.56);
      joy.moveTo("rz", 0.343);
      joy.moveTo("rz", 0.754);
      joy.moveTo("x", 0.56);
      joy.moveTo("x", 0.343);
      joy.moveTo("x", 0.754);
      joy.moveTo("y", 0.56);
      joy.moveTo("y", 0.343);
      joy.moveTo("y", 0.754);

      joy.pressReleaseButton("3");
      joy.pressButton("10");
      // Mqtt mqtt01 = (Mqtt) Runtime.start("mqtt01", "Mqtt");
      // WatchDogTimer watchdog = (WatchDogTimer) Runtime.start("watchdog",
      // "WatchDogTimer");
      // Python python = (Python) Runtime.start("python", "Python");

      // Motor m1 = (Motor) Runtime.start("m1", "Motor");

      // configuration
      // adding and activating a checkpoint
      // watchdog.addTimer("joystickCheck"); // <-- response action
      // watchdog.addTimer("joystickCheck",

      // watchdog.addAction("m1", "stop");

      // joy.mapId("x", "rx");
      // joy.map("y", -1, 1, 0, 180);
     
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  public void moveTo(String axisName, float value) {
    moveTo(axisName, (double) value);
  }

  public void moveTo(String axisName, double value) {
    if (components == null) {
      log.error("controller not set");
      return;
    }

    if (components == null) {
      log.error("components are null");
      return;
    }

    if (!components.containsKey(axisName)) {
      log.error("component {} does not exist", axisName);
      return;
    }

    Component component = components.get(axisName);
    component.setVirtualValue(value);
  }

}
