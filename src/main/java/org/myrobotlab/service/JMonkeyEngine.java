package org.myrobotlab.service;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.jme3.Jme3App;
import org.myrobotlab.jme3.Jme3Object;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.virtual.VirtualMotor;
import org.slf4j.Logger;

import com.google.gson.internal.LinkedTreeMap;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

public class JMonkeyEngine extends Service {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);

  AssetManager assetManager;
  InputManager inputManager;
  FlyByCamera flyCam;
  Camera cam;
  AnalogListener analogListener;
  ViewPort viewPort;
  Node rootNode;
  AppSettings settings;

  long startUpdateTs;
  long deltaMs;
  long sleepMs;

  public void updatePosition(String name, Double angle) {
    Move move = new Move(name, angle);
    eventQueue.add(move);
  }

  public void updatePosition(Move move) {
    eventQueue.add(move);
  }

  public class Move {
    String name;
    // Vector3f rotationMask - can send a rotational mask ? vs using the
    // Jme3Object ?
    Double deltaAngle;// relative change

    public Move(String name, Double deltaAngle) {
      this.name = name;
      this.deltaAngle = deltaAngle;
    }

    public String getName() {
      return name;
    }

    public Double getAngle() {
      return deltaAngle;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(name);
      sb.append(" ");
      sb.append(deltaAngle);
      return sb.toString();
    }
  }

  protected Queue<Move> eventQueue = new ConcurrentLinkedQueue<Move>();

  protected transient Map<String, Jme3Object> nodes = new TreeMap<String, Jme3Object>();

  // TODO - make intermediate class - which has common interface to grab
  // shapes/boxes
  transient Jme3App app;

  String defaultAppType = "Jme3App";

  boolean autoAttach = true;

  boolean autoAttachAll = true;

  public JMonkeyEngine(String n) {
    super(n);
  }

  public void startService() {
    super.startService();
    // notify me if new services are created
    subscribe(Runtime.getRuntimeName(), "registered");
    List<ServiceInterface> services = Runtime.getServices();

    // for all apps "attach" all services
    // for (apps.size())

  }

  // FIXME - Possible to have a "default" simple servo app - with default
  // service script
  public SimpleApplication start() {
    return start(defaultAppType, defaultAppType);
  }

  // dynamic create of type... TODO fix name start --> create
  synchronized public SimpleApplication start(String appName, String appType) {
    if (app == null) {
      // create app
      if (!appType.contains(".")) {
        appType = String.format("org.myrobotlab.jme3.%s", appType);
      }

      Jme3App newApp = (Jme3App) Instantiator.getNewInstance(appType, this);
      if (newApp == null) {
        error("could not instantiate simple application %s", appType);
        return null;
      }

      app = newApp;

      // start it with "default" settings
      settings = new AppSettings(true);
      settings.setResolution(1024, 768);
      // settings.setEmulateMouse(false);
      // settings.setUseJoysticks(false);
      settings.setUseInput(true);
      settings.setAudioRenderer(null);
      app.setSettings(settings);
      app.setShowSettings(false);
      app.setPauseOnLostFocus(false);
      analogListener = new InputListener();

      app.start();
      return app;
    }
    info("already started app %s", appType);
    return app;
  }

  class InputListener implements AnalogListener {

    @Override
    public void onAnalog(String name, float keyPressed, float tpf) {
      if (name.equals("MouseClickL")) {
        // rotate+= keyPressed;
        rootNode.rotate(0, -keyPressed, 0);
        // log.info(rotate);
      } else if (name.equals("MouseClickR")) {
        // rotate+= keyPressed;
        rootNode.rotate(0, keyPressed, 0);
        // log.info(rotate);
      } else if (name.equals("MMouseUp") || name.equals("ZoomIn")) {
        rootNode.setLocalScale(rootNode.getLocalScale().mult(1.01f));
      } else if (name.equals("MMouseDown") || name.equals("ZoomOut")) {
        rootNode.setLocalScale(rootNode.getLocalScale().mult(0.99f));
      } else if (name.equals("Up")) {
        rootNode.move(0, keyPressed * 100, 0);
      } else if (name.equals("Down")) {
        rootNode.move(0, -keyPressed * 100, 0);
      } else if (name.equals("Left")) {
        rootNode.move(-keyPressed * 100, 0, 0);
      } else if (name.equals("Right")) {
        rootNode.move(keyPressed * 100, 0, 0);
      }
      // seem no worky
      else if (name.equals("FullScreen")) {
        if (settings.isFullscreen()) {
          settings.setFullscreen(false);
        } else {
          settings.setFullscreen(true);
        }
      }
    }

  }

  @Override
  public void stopService() {
    super.stopService();
    try {
      if (app != null) {
        app.getRootNode().detachAllChildren();
        app.getGuiNode().detachAllChildren();
        app.stop();
      }
      // app.destroy();
    } catch (Exception e) {
      log.error("releasing jme3 app threw", e);
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

    ServiceType meta = new ServiceType(JMonkeyEngine.class.getCanonicalName());
    meta.addDescription("is a 3d game engine, used for simulators");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // TODO: extract version numbers like this into a constant/enum
    String jmeVersion = "3.2.0-stable";
    meta.addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-niftygui", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-bullet", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-bullet-native", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-niftygui", jmeVersion);
    // jbullet ==> org="net.sf.sociaal" name="jME3-jbullet" rev="3.0.0.20130526"

    // audio dependencies
    meta.addDependency("de.jarnbjo", "j-ogg-all", "1.0.0");

    meta.addCategory("simulator");
    return meta;
  }

  public Jme3App getApp() {
    return app;
  }

  // auto Register
  public void onRegistered(Service service) throws Exception {
    // new service - see if we can virtualize it
    log.info("{}.onRegistered({})", getName(), service);
    if (autoAttach) {
      if (autoAttachAll) {
        // spin through all apps - attempt to attach
      }
      attach(service);
    }
  }

  public void attach(Attachable service) throws Exception {
    if (app == null) {
      start(); // FIXME - start a default app
    }
    app.attach(service);
  }

  public static void main(String[] args) {
    try {

      // FIXME - ADD DEFAULT ROTATION !!! ... default"move" & default"rotate" &
      // default rotationalMask !!!
      // FIXME - something to autoAttach ! ... even at origin position

      LoggingFactory.init(Level.INFO);

      JMonkeyEngine jme = (JMonkeyEngine) Runtime.start("jme", "JMonkeyEngine");
      // jme.startServoController("i01.left"); // GAH won't work :(
      // jme.startServoController("i01.right");

      //
      // Runtime.start("i01.left", "Jme3ServoController"); GAH won't work :(
      // Runtime.start("i01.right", "Jme3ServoController");

      VirtualServoController vsc = (VirtualServoController) Runtime.start("i01.left", "VirtualServoController");
      vsc.attachSimulator(jme);
      vsc = (VirtualServoController) Runtime.start("i01.right", "VirtualServoController");
      vsc.attachSimulator(jme);

      InMoov i01 = (InMoov) Runtime.create("i01", "InMoov");// has attach ...
                                                            // runtime does
                                                            // dynamic binding
                                                            // anyway...
      InMoovHead head = i01.startHead("COM98");
      Servo s = (Servo) Runtime.getService("i01.head.rothead");
      Servo jaw = (Servo) Runtime.getService("i01.head.jaw");

      // absolute jme movements
      /**
       * <pre>
       * jme.updatePosition("i01.head.jaw", 70.0);
       * jme.updatePosition("i01.head.jaw", 80.0);
       * jme.updatePosition("i01.head.jaw", 90.0);
       * jme.updatePosition("i01.head.jaw", 100.0);
       * 
       * jme.updatePosition("i01.head.rothead", 90.0);
       * jme.updatePosition("i01.head.rothead", 70.0);
       * jme.updatePosition("i01.head.rothead", 85.0);
       * jme.updatePosition("i01.head.rothead", 130.0);
       * // head.moveTo(90, 90);
       * </pre>
       */

      // is this necessary ???
      head.rest(); // <- WRONG should not have to do this .. it should be
                   // assumed :P
      // FIXME - there has to be a "default" speed for virtual servos
      s.setVelocity(40);
      s.moveTo(0); // goes to 30 for rothead - because "min" <-- WRONG 0 should
                   // be 30 .. but start position should be 90 !!!
      s.moveTo(180);
      s.moveTo(90);
      s.moveTo(0);
      for (int i = 90; i < 180; ++i) {
        /// head.moveTo(i, i);
        s.moveTo(i);
        sleep(100);
      }

      // i01.startAll("COM98", "COM99");
      // FIXME - fix dna/peer api
      log.info("InMoov buildDnaKeys [{}]", i01.buildDnaKeys("i01", "InMoov"));
      log.info("InMoov getDnaString [{}]", InMoov.getDnaString());

      jme.releaseService();

      // NO NO NO - listen to Runtime , startup create all that can be created
      // listen to Runtime - create new for anything which appears to be new and
      // can be created
      // attach a simulator to a virtual device
      // which listens on a serial port
      // virtual.attach(jme3);

      // NO NO NO X 2 - there is no spatial information @ Runtime nor when a
      // service is newly created
      // So it would be better if the "Simulator" could ingest configuration and
      // bind its objects by name
      // 'binding' - bind by name, but a typed reference is a good start

      // jme3.create(servo);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  // FIXME - i suspect "controllers" really should be virtualized not control
  // services !
  // the challenge is to have "both" at the same time real & virtualized
  /*
   * @Override public VirtualServo createVirtualServo(String name) { return
   * currentApp.createVirtualServo(name); }
   */
  /*
   * @Override public Object create(ServiceInterface service) { // TODO
   * Auto-generated method stub return null; }
   */

  public VirtualMotor createVirtualMotor(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  public void addMsg(Move move) {
    // app.getEventQueue().add(msg);
    eventQueue.add(move);
  }

  public Queue<Move> getEventQueue() {
    return eventQueue;
  }

  public void putNode(Node node) {
    putNode(node.getName(), node);
  }

  public Jme3Object putNode(String name, Node node) {
    return putNode(name, node, null);
  }

  public Jme3Object putNode(String name, Node node, Mapper mapper) {
    return nodes.put(name, new Jme3Object(node, mapper));
  }

  public Jme3Object putNode(String name, Node node, Mapper mapper, Vector3f defaultRotationAxis) {
    return nodes.put(name, new Jme3Object(node, mapper, defaultRotationAxis));
  }

  public Node getNode(String name) {
    Jme3Object o = getJme3Object(name);
    if (o != null) {
      return o.getNode();
    }
    return null;
  }

  public Jme3Object getJme3Object(String name) {
    if (nodes.containsKey(name)) {
      return nodes.get(name);
    }
    return null;
  }

  public void simpleInitApp() {

    // wtf - assetManager == null - another race condition ?!?!?
    // after start - these are initialized as "default"
    assetManager = app.getAssetManager();
    inputManager = app.getInputManager();
    flyCam = app.getFlyByCamera();
    cam = app.getCamera();
    viewPort = app.getViewPort();
    rootNode = app.getRootNode();

    inputManager.setCursorVisible(true);
    flyCam.setEnabled(false);
    cam.setLocation(new Vector3f(0f, 0f, 900f));

    assetManager.registerLocator("InMoov/jm3/assets", FileLocator.class);

    inputManager.addMapping("MouseClickL", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(analogListener, "MouseClickL");
    inputManager.addMapping("MouseClickR", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(analogListener, "MouseClickR");
    inputManager.addMapping("MMouseUp", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
    inputManager.addListener(analogListener, "MMouseUp");
    inputManager.addMapping("MMouseDown", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
    inputManager.addListener(analogListener, "MMouseDown");
    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT)); // A
                                                                                                        // and
                                                                                                        // left
                                                                                                        // arrow
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT)); // D
                                                                                                          // and
                                                                                                          // right
                                                                                                          // arrow
    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP)); // A
                                                                                                    // and
                                                                                                    // left
                                                                                                    // arrow
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN)); // D
                                                                                                        // and
                                                                                                        // right
                                                                                                        // arrow
    inputManager.addMapping("ZoomIn", new KeyTrigger(KeyInput.KEY_E));
    inputManager.addMapping("ZoomOut", new KeyTrigger(KeyInput.KEY_Q));
    inputManager.addListener(analogListener, new String[] { "Left", "Right", "Up", "Down", "ZoomIn", "ZoomOut" });
    // no worky
    inputManager.addMapping("FullScreen", new KeyTrigger(KeyInput.KEY_F));
    inputManager.addListener(analogListener, "FullScreen");

    viewPort.setBackgroundColor(ColorRGBA.Gray);

    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
    rootNode.addLight(sun);
    rootNode.scale(.5f);
    rootNode.setLocalTranslation(0, -200, 0);

    // FIXME NOT i01 !! - if a InMoov Type has been identified
    // the InMoov {name} is pulled
    // FIXME - correct names of types :P
    // FIXME - INPUT ALL THIS VIA TEXT/yaml/json config !!!

    putNode("rootNode", rootNode);
    putNode("i01.torso.lowStom", "rootNode", "Models/ltorso.j3o", null, Vector3f.UNIT_X.mult(1), new Vector3f(0, 0, 0), 0);

    // FIXME - bind this to process Peers !!!! "default" "Peer" info in 3D form

    putNode("i01.torso.midStom", "i01.torso.lowStom", "Models/mtorso.j3o", new Mapper(0, 180, 120, 60), Vector3f.UNIT_Y.mult(-1), new Vector3f(0, 0, 0), 0);
    putNode("i01.torso.topStom", "i01.torso.midStom", "Models/ttorso1.j3o", new Mapper(0, 180, 80, 100), Vector3f.UNIT_Z.mult(1), new Vector3f(0, 105, 10), 0);
    putNode("rightS", "i01.torso.topStom", null, null, Vector3f.UNIT_Z.mult(1), new Vector3f(0, 300, 0), 0);

    // Vector3f angle = rotationMask.mult((float) Math.toRadians(6));
    putNode("i01.rightArm.omoplate", "rightS", "Models/Romoplate1.j3o", new Mapper(0, 180, 10, 70), Vector3f.UNIT_Z.mult(-1), new Vector3f(-143, 0, -17), 0);

    // angle = rotationMask.mult((float) Math.toRadians(-2));
    // node.rotate(angle.x, angle.y, angle.z); <------------ additional rotation
    // ...
    putNode("i01.rightArm.shoulder", "i01.rightArm.omoplate", "Models/Rshoulder1.j3o", new Mapper(0, 180, 0, 180), Vector3f.UNIT_X.mult(-1), new Vector3f(-23, -45, 0), 0);
    putNode("i01.rightArm.rotate", "i01.rightArm.shoulder", "Models/rotate1.j3o", new Mapper(0, 180, 40, 180), Vector3f.UNIT_Y.mult(-1), new Vector3f(-57, -55, 8), 0);

    // angle = rotationMask.mult((float) Math.toRadians(30)); // additional
    // rotate !
    // node.rotate(angle.x, angle.y, angle.z);
    putNode("i01.rightArm.bicep", "i01.rightArm.rotate", "Models/Rbicep1.j3o", new Mapper(0, 180, 5, 60), Vector3f.UNIT_X.mult(-1), new Vector3f(5, -225, -32), 0);
    putNode("leftS", "i01.torso.topStom", "Models/Lomoplate1.j3o", null, Vector3f.UNIT_Z.mult(1), new Vector3f(0, 300, 0), 0);

    // angle = rotationMask.mult((float) Math.toRadians(4));
    // node.rotate(angle.x, angle.y, angle.z); <-- another rotation ...
    putNode("i01.leftArm.omoplate", "leftS", "Models/Lomoplate1.j3o", new Mapper(0, 180, 10, 70), Vector3f.UNIT_Z.mult(1), new Vector3f(143, 0, -15), 0);
    putNode("i01.leftArm.shoulder", "i01.leftArm.omoplate", "Models/Lshoulder.j3o", new Mapper(0, 180, 0, 180), Vector3f.UNIT_X.mult(-1), new Vector3f(17, -45, 5), 0);
    putNode("i01.leftArm.rotate", "i01.leftArm.shoulder", "Models/rotate1.j3o", new Mapper(0, 180, 40, 180), Vector3f.UNIT_Y.mult(1), new Vector3f(65, -58, -3), 0);

    // angle = rotationMask.mult((float) Math.toRadians(27));
    // node.rotate(angle.x, angle.y, angle.z); <-------------- additional rotate
    // !!!
    putNode("i01.leftArm.bicep", "i01.leftArm.rotate", "Models/Lbicep.j3o", new Mapper(0, 180, 5, 60), Vector3f.UNIT_X.mult(-1), new Vector3f(-14, -223, -28), 0);

    // angle = rotationMask.mult((float) Math.toRadians(-90)); <- Ha .. and an
    // additional rotate
    // node.rotate(angle.x, angle.y, angle.z);
    putNode("i01.rightHand.wrist", "i01.rightArm.bicep", "Models/RWristFinger.j3o", new Mapper(0, 180, 130, 40), Vector3f.UNIT_X.mult(-1), new Vector3f(15, -290, -10), 0);

    // angle = rotationMask.mult((float) Math.toRadians(-90)); <--- HA .. and
    // additional rotation !
    // node.rotate(angle.x, angle.y, angle.z);
    putNode("i01.leftHand.wrist", "i01.leftArm.bicep", "Models/LWristFinger.j3o", new Mapper(0, 180, 40, 130), Vector3f.UNIT_Y.mult(1), new Vector3f(0, -290, -20), 90);
    putNode("i01.head.neck", "i01.torso.topStom", "Models/neck.j3o", new Mapper(0, 180, 60, 110), Vector3f.UNIT_X.mult(-1), new Vector3f(0, 452.5f, -45), 0);
    putNode("i01.head.rollNeck", "i01.head.neck", null, new Mapper(0, 180, 60, 115), Vector3f.UNIT_Z.mult(1), new Vector3f(0, 0, 0), 90);
    putNode("i01.head.rothead", "i01.head.rollNeck", "Models/head.j3o", new Mapper(0, 180, 150, 30), Vector3f.UNIT_Y.mult(-1), new Vector3f(0, 10, 20), 90);
    putNode("i01.head.jaw", "i01.head.rothead", "Models/jaw.j3o", new Mapper(0, 180, 0, 180), Vector3f.UNIT_X.mult(-1), new Vector3f(-5, 60, -50), 90);

    save("inmoov-jme.json");

    // Vector3D vector = new Vector3D(7, 3, 120);
    load("inmoov-jme.json");

    save("inmoov-jme1.json");
  }

  public void simpleUpdate(float tpf) {

    // start the clock on how much time we will take
    startUpdateTs = System.currentTimeMillis();

    while (eventQueue.size() > 0) {
      Move move = null;
      try {

        // TODO - support relative & absolute moves
        move = eventQueue.remove();

        String name = move.getName();

        Jme3Object object = getJme3Object(name);

        if (object == null) {
          log.error("no Jme3Object named {}", name);
        }
        object.rotateDegrees(move.getAngle());

      } catch (Exception e) {
        log.error("simpleUpdate failed for {} - targetName", move, e);
      }
    }

    deltaMs = System.currentTimeMillis() - startUpdateTs;
    sleepMs = 33 - deltaMs;
    sleep(sleepMs);
  }

  public Jme3Object putNode(String name, String parentName, String assetPath, Mapper mapper, Vector3f rotationMask, Vector3f localTranslation, double currentAngle) {
    if (nodes.containsKey(name)) {
      log.error("there is already a node named {}", name);
    }
    return nodes.put(name, new Jme3Object(this, name, parentName, assetPath, mapper, rotationMask, localTranslation, currentAngle));
  }

  public boolean load(String jsonPath) {
    try {
      String json = FileIO.toString(jsonPath);
      Map<String, Object> list = CodecUtils.fromJson(json, nodes.getClass());
      for (String name : list.keySet()) {
        String nodePart = CodecUtils.toJson(list.get(name));
        Jme3Object node = CodecUtils.fromJson(nodePart, Jme3Object.class);
        // get/create transient parts
        // node.setService(Runtime.getService(name));
        // node.setJme(this);
        // putN
        // nodes.put(node.getName(), node);
      }
      return true;
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  public boolean save(String jsonPath) {
    try {
      String json = CodecUtils.toJson(nodes);
      FileIO.toFile(jsonPath, json.getBytes());
      return true;
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  public Spatial loadModel(String assetPath) {
    return assetManager.loadModel(assetPath);
  }

}
