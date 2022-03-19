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
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.joystick.Component;
import org.myrobotlab.joystick.Controller;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.service.config.JoystickConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.AnalogData;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.interfaces.AnalogListener;
import org.myrobotlab.service.interfaces.AnalogPublisher;
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
public class Joystick extends Service implements AnalogPublisher {

  public final static Logger log = LoggerFactory.getLogger(Joystick.class);
  private static final long serialVersionUID = 1L;

  protected List<Controller> controllers;

  /**
   * current selected controller
   */
  protected Controller hardwareController = null;

  /**
   * component listeners
   */
  protected Map<String, Set<MRLListener>> idAndServiceSubscription = new HashMap<>();

  /**
   * all analog listeners
   */
  protected Map<String, Set<String>> analogListeners = new HashMap<>();

  /**
   * all digital listeners
   */
  protected Map<String, Set<String>> digitalListeners = new HashMap<>();

  protected List<Component> hardwareComponents;

  /**
   * non serializable hardware rumblers
   */
  protected transient Rumbler[] hardwareRumblers;

  final protected Poller poller = new Poller();

  /**
   * polling state
   */
  protected boolean isPolling = false;

  /**
   * name to index map of controllers
   */
  protected TreeMap<String, Integer> controllerNames = new TreeMap<String, Integer>();

  /**
   * index for the rumbler being used
   */
  protected int rumblerIdx;

  /**
   * is rumbler on or off
   */
  protected boolean rumblerOn = false;

  /**
   * non-transient serializable definition
   */
  protected Map<String, MapperLinear> mappers = new HashMap<String, MapperLinear>();

  protected Map<String, Component> components = null;

  protected String controller;

  public class Poller implements Runnable {

    transient Thread myThread = null;

    public void run() {
      poll();
    }

    public synchronized void start() {
      if (isPolling) {
        log.warn("already polling");
        return;
      }
      myThread = new Thread(this, String.format("%s_polling", getName()));
      myThread.start();
    }

    public synchronized void stop() {
      isPolling = false;
    }
  }

  /**
   * main polling loop - data read and published
   */
  public void poll() {

    Controller pollingController = null;
    Component[] hwComponents = null;
    isPolling = true;
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
          break;
        }

        // get the data
        if (isPolling && !pollingController.poll()) {
          error("failed to poll controller");
          stopPolling();
          break;
        }

        // iterate through each component and compare last values
        for (int i = 0; i < hwComponents.length; i++) {

          Component hwComp = hwComponents[i];

          String id = hwComp.getIdentifier().toString();
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
              input = Double.valueOf(mappers.get(id).calcOutput((double) input)).floatValue();
            }

            JoystickData data = new JoystickData(id, input);
            invoke("publishJoystickInput", data);

            // filtered by analog and id
            if (analogListeners.containsKey(id)) {
              Set<String> listeners = analogListeners.get(id);
              AnalogData d = new AnalogData();
              d.id = id;
              d.name = getName();
              d.value = data.value.doubleValue();
              for (String listener : listeners) {
                send(listener, "onAnalog", d);
              }
            }

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
      }
    } // while
    isPolling = false;
  }

  public Joystick(String n, String id) {
    super(n, id);
  }

  // FIXME - simply set components e.g. getComponents
  public Map<String, Component> getComponents() {
    components = new HashMap<String, Component>();
    if (hardwareController == null) {
      info("getComponents no controller set");
      return components;
    }
    components = hardwareController.getComponentMap();
    return components;
  }

  public void refresh() {
    getControllers();
    getComponents();
    broadcastState();
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
    MapperLinear mapper = new MapperLinear(x0, x1, y0, y1);
    mappers.put(name, mapper);
  }

  public void addInputListener(Service service) {
    service.subscribe(this.getName(), "publishJoystickInput");
  }

  @Override
  public void attach(Attachable service) {
    if (AnalogListener.class.isAssignableFrom(service.getClass())) {
      attachAnalogListener((AnalogListener) service);
    } else {
      error(String.format("%s.attach does not know how to attach to a %s", this.getClass().getSimpleName(), service.getClass().getSimpleName()));
    }
  }

  public void attachAnalogListener(AnalogListener service) {
    String id = service.getAnalogId();
    String serviceName = service.getName();
    getComponents();
    if (components != null && !components.containsKey(id)) {
      error("%s requests subscription to component %s - but %s does not exist", serviceName, id, id);
    }

    Component c = components.get(id);
    if (c == null) {
      error("could not find requested joystick component %s", id);
    }
    if (c != null && !c.isAnalog) {
      error("attachAnalogListener getAnalogId (%s) is a not an analog component", id);
    }

    Set<String> listeners = null;
    if (analogListeners.containsKey(id)) {
      listeners = analogListeners.get(id);
      if (listeners.contains(serviceName)) {
        log.info("already attached to %s", serviceName);
        return;
      }
    } else {
      listeners = new HashSet<String>();
    }
    analogListeners.put(id, listeners);
    listeners.add(serviceName);
    // service.attachAnalogPublisher(this);
  }

  @Override
  public void detachAnalogListener(AnalogListener listener) {
    String id = listener.getAnalogId();
    String serviceName = listener.getName();
    Set<String> listeners = analogListeners.get(id);
    if (listeners != null) {
      listeners.remove(serviceName);
    }
  }

  @Deprecated /* name should be attachComponentListener */
  public void attach(String serviceName, String id) {
    if (!components.containsKey(id)) {
      error("%s requests subscription to component %s - but %s does not exist", serviceName, id, id);
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

  public void startPolling() {
    log.info("startPolling - starting new polling thread {}_polling", getName());
    poller.start();
  }

  public void stopPolling() {
    poller.stop();
  }

  public void startService() {
    super.startService();
    initNativeLibs();
    invoke("getControllers");
  }

  private void initNativeLibs() {
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

  public String getController() {
    return controller;
  }

  public void releaseService() {
    super.releaseService();
    stopPolling();    
  }

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

    String json = CodecUtils.toJson(controller);
    FileIO.toFile(filename, json);

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
    try {

      Runtime.main(new String[] { "--id", "admin", "--from-launcher" });
      LoggingFactory.init("INFO");

      Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
      Runtime.start("webgui", "WebGui");

      boolean done = true;
      if (done) {
        return;
      }

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

  @Override
  public ServiceConfig getConfig() {
    JoystickConfig config = new JoystickConfig();
    config.controller = controller;

    if (analogListeners.size() > 0) {
      config.analogListeners = new HashMap<>();
      for (String key : analogListeners.keySet()) {
        Set<String> listeners = analogListeners.get(key);
        // HashSet<String> s = new HashSet<>();
        // String[] s = new String[listeners.size()];
        ArrayList<String> s = new ArrayList<>();
        config.analogListeners.put(key, s);
        for (String l : listeners) {
          s.add(l);
        }
      }
    }
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {

    // "special" needs native libs
    initNativeLibs();

    // scan for hardware controllers
    // required because you can't "set" a controller
    // unless its in the list of controllers
    getControllers();

    // get controller request from config
    JoystickConfig config = (JoystickConfig) c;
    if (config.controller != null) {
      setController(config.controller);
    }

    // stupid transform from array to set - yaml wants array, set prevents
    // duplicates :(
    if (config.analogListeners != null) {
      for (String id : config.analogListeners.keySet()) {
        ArrayList<String> list = config.analogListeners.get(id);
        Set<String> s = analogListeners.get(id);
        if (s == null) {
          s = new HashSet<>();
          analogListeners.put(id, s);
          // attachAnalogListener(null);
        }
        s.addAll(list);
      }
    }
    return c;
  }

  @Override
  public AnalogData publishAnalog(AnalogData data) {
    return data;
  }

}
