package org.myrobotlab.service;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.cv.CvData;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.jme3.AnalogHandler;
import org.myrobotlab.jme3.HudText;
import org.myrobotlab.jme3.Interpolator;
import org.myrobotlab.jme3.Jme3App;
import org.myrobotlab.jme3.Jme3Msg;
import org.myrobotlab.jme3.Jme3Util;
import org.myrobotlab.jme3.MainMenuState;
import org.myrobotlab.jme3.Search;
import org.myrobotlab.jme3.UserData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.abstracts.AbstractComputerVision;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.swing.ServiceGui;
import org.slf4j.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
// import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.collision.CollisionResults;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.plugins.blender.BlenderLoader;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;

/**
 * A simulator built on JMonkey 3 Engine.
 * 
 * FIXME - use gateway analogy ! A simulator should be treated as a gateway, the
 * service "twins" that are represented inside are "remote". Some services like
 * UIs (webgui and swinggui) dynamically create "remote" services and allow the
 * current process to interact with them the same way they would with other
 * remote (networked) services.
 * 
 * @author GroG, calamity, kwatters, moz4r and many others ...
 *
 */
public class JMonkeyEngine extends Service implements Gateway, ServoController, ActionListener, Simulator, IKJointAngleListener {

  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);

  private static final long serialVersionUID = 1L;

  transient Map<String, List<ServiceGui>> nameMethodCallbackMap = new HashMap<String, List<ServiceGui>>();

  @Deprecated /* came from jme3ServoController... */
  Map<String, ServoControl> servos = new TreeMap<String, ServoControl>();

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(JMonkeyEngine.class);
    meta.addDescription("is a 3d game engine, used for simulators");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // TODO: extract version numbers like this into a constant/enum
    String jmeVersion = "3.2.2-stable";
    meta.addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);
    // meta.addDependency("org.jmonkeyengine", "jme3-test-data", jmeVersion);
    meta.addDependency("com.simsilica", "lemur", "1.11.0");
    meta.addDependency("com.simsilica", "lemur-proto", "1.10.0");

    meta.addDependency("org.jmonkeyengine", "jme3-bullet", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-bullet-native", jmeVersion);

    // meta.addDependency("jme3utilities", "Minie", "0.6.2");

    // "new" physics - ik forward kinematics ...

    // not really supposed to use blender models - export to j3o
    meta.addDependency("org.jmonkeyengine", "jme3-blender", jmeVersion);

    // jbullet ==> org="net.sf.sociaal" name="jME3-jbullet" rev="3.0.0.20130526"

    // audio dependencies
    meta.addDependency("de.jarnbjo", "j-ogg-all", "1.0.0");

    meta.addCategory("simulator");
    return meta;
  }

  boolean altLeftPressed = false;

  double defaultServoSpeed = 60;

  transient AnalogListener analog = null;

  transient Jme3App app;

  transient AssetManager assetManager;

  String assetsDir = getDataDir() + File.separator + "assets";

  boolean autoAttach = true;

  // transient BulletAppState bulletAppState;

  final static String CAMERA = "camera";
  final static String ROOT = "root";

  transient Node camera = new Node(CAMERA);

  transient Vector3f cameraDirection = new Vector3f();

  transient Camera cameraSettings;

  transient CameraNode camNode;
  boolean ctrlLeftPressed = false;
  String defaultAppType = "Jme3App";

  long deltaMs;

  transient DisplayMode displayMode = null;

  transient FlyByCamera flyCam;

  String fontColor = "#66ff66"; // green

  int fontSize = 14;
  boolean fullscreen = false;

  transient Node guiNode;
  transient Map<String, HudText> guiText = new TreeMap<>();

  int height = 768;
  transient AtomicInteger id = new AtomicInteger();
  transient InputManager inputManager;
  transient Interpolator interpolator;
  protected Queue<Jme3Msg> jme3MsgQueue = new ConcurrentLinkedQueue<Jme3Msg>();
  final public String KEY_SEPERATOR = "/";

  transient DisplayMode lastDisplayMode = null;

  transient MainMenuState menu;

  String modelsDir = assetsDir + File.separator + "Models";

  boolean mouseLeftPressed = false;

  boolean mouseRightPressed = false;

  final Map<String, String> nameMappings = new TreeMap<String, String>();

  transient Map<String, Spatial> nodesx = new TreeMap<String, Spatial>();

  // https://stackoverflow.com/questions/16861727/jmonkey-engine-3-0-drawing-points
  FloatBuffer pointCloudBuffer = null;

  transient Material pointCloudMat = null;

  transient Mesh pointCloudMesh = new Mesh();

  transient Node rootNode;

  transient Spatial selectedForMovement = null;

  transient Spatial selectedForView = null;

  int selectIndex = 0;

  transient AppSettings settings;

  boolean shiftLeftPressed = false;

  long sleepMs;

  long startUpdateTs;

  transient AppStateManager stateManager;

  transient Jme3Util util;

  transient ViewPort viewPort;

  int width = 1024;

  private String guiId;

  List<Jme3Msg> history = new ArrayList<Jme3Msg>();
  boolean saveHistory = false;

  public JMonkeyEngine(String n, String id) {
    super(n, id);
    File d = new File(modelsDir);
    d.mkdirs();
    util = new Jme3Util(this);
    analog = new AnalogHandler(this);
    interpolator = new Interpolator(this, util);

    guiId = "jme-" + getName() + "-" + getId();

    // setup the virtual reflection
    // this will "connect" to our mrl instance 
    // and part of the connection is the mrl instance
    // sending a series of registrations ... including self
    // still a race condition ?
    try {
      connect("jme://local/messages");
    } catch (Exception e) {
    }

    // process existing registrations

    Runtime runtime = Runtime.getInstance();
    for (Registration registration : runtime.getServiceList()) {
      try {
        onRegistered(registration);
      } catch (Exception e) {
        error(e);
      }

    }
  }

  public void addNode(String name) {
    addMsg("addNode", name);
  }

  public void addBox(String boxName) {
    addBox(boxName, 1f, 1f, 1f); // room box
  }

  public void addBox(String boxName, double width, double depth, double height) {
    addBox(boxName, width, depth, height, null, null);
    moveTo(boxName, 0f, height, 0f); // center it on the floor fully above the
                                     // ground
  }

  // FIXME make method "without" name to be added to the _system_box node ..
  public Node addBox(String name, Double width, Double depth, Double height, String color, Boolean fill) {

    Node boxNode = null;
    Spatial check = find(name);

    if (check != null && check instanceof Geometry) {
      log.error("addBox - scene graph already has {} and it is a Geometry", check);
      return null;
    } else if (check != null && check instanceof Node) {
      boxNode = (Node) check;
      return boxNode;
    } else {
      boxNode = new Node(name);
    }

    if (width == null) {
      width = 1.0;
    }

    if (depth == null) {
      depth = 1.0;
    }

    if (height == null) {
      height = 1.0;
    }

    Box box = new Box(width.floatValue(), depth.floatValue(), height.floatValue());

    // wireCube.setMode(Mesh.Mode.LineLoop);
    // box.setMode(Mesh.Mode.Lines);
    // FIXME - geom & matterial always xxx-geometry ? & xxx-material ??
    Geometry geom = new Geometry(String.format("%s._geometry", name), box);

    Material mat1 = null;

    if (fill == null || fill.equals(false)) {
      // mat1 = new Material(assetManager,
      // "Common/MatDefs/Light/Lighting.j3md");
      mat1 = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
      box.setMode(Mesh.Mode.Lines);
    } else {
      mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
      mat1.setColor("Color", Jme3Util.toColor(color));
    }

    // mat1 = new Material(assetManager, "Common/MatDefs/Light/Deferred.j3md");
    // mat1.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Front);
    geom.setMaterial(mat1);

    boxNode.attachChild(geom);

    // FIXME - optimize rootNode/geom/nodes & jme3Node !
    // UserData o = new UserData(this, boxNode);
    // nodes.put(name, o);
    rootNode.attachChild(boxNode);
    moveTo(name, 0.0f, 0.5f * height, 0.0f);
    // index(boxNode);

    return boxNode;
  }

  public void addGrid(String name) {
    addGrid(name, new Vector3f(0, 0, 0), 40, "CCCCCC");
  }

  public void addGrid(String name, Vector3f pos, int size, String color) {
    Spatial s = find(name);
    if (s != null) {
      log.warn("addGrid {} already exists");
      return;
    }
    Geometry g = new Geometry("wireframe grid", new Grid(size, size, 1.0f));
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.getAdditionalRenderState().setWireframe(true);
    mat.setColor("Color", Jme3Util.toColor(color));
    g.setMaterial(mat);
    g.center().move(pos);
    Node n = new Node(name);
    n.attachChild(g);
    rootNode.attachChild(n);
  }

  public void addMsg(String method, Object... params) {
    jme3MsgQueue.add(new Jme3Msg(method, params));
  }

  public void attach(Attachable service) throws Exception {
    attach(service, (String[]) null);
  }

  public void attach(Attachable service, String... nodeNames) throws Exception {
    attach(service.getName(), nodeNames);
  }

  Map<String, String[]> multiMapped = new TreeMap<String, String[]>();

  public void attach(String name, String... nodeNames) throws Exception {

    ServiceInterface service = Runtime.getService(name);
    if (service == null) {
      log.error("{} not found in registry", name);
      return;
    }

    if (nodeNames != null) {
      multiMapped.put(name, nodeNames);
    }

    // We do type evaluation and routing based on string values vs instance
    // values
    // this is to support future (non-Java) classes that cannot be instantiated
    // and
    // are subclassed in a proxy class with getType() overloaded for to identify
    if (service.getType().equals("org.myrobotlab.service.OpenCV")) {
      AbstractComputerVision cv = (AbstractComputerVision) service;
      subscribe(service.getName(), "publishCvData");
    }

    if (service.getType().equals("org.myrobotlab.service.Servo")) {
      // what to do here - there are several options 
      ServoControl sc = (ServoControl)Runtime.getService(name);
      sc.attachServoController(getName(), null, null, sc.getMaxSpeed());
    }

    // backward attach ?
  }

  public void attachChild(Spatial node) {
    rootNode.attachChild(node);
  }

  /**
   * binds two objects together ...
   * 
   * @param child
   * @param parent
   */
  public void bind(String child, String parent) {
    addMsg("bind", child, parent);
  }

  public Map<String, UserData> buildTree() {
    TreeMap<String, UserData> tree = new TreeMap<String, UserData>();
    return buildTree(tree, "", rootNode, false, false);
  }

  /**
   * The buildTree method creates a data structure for quick access and indexing
   * of nodes - it can build it in two different ways - one which uses full
   * depth for an access key useDepth=true and another that is a flat model.
   * Both can have collisions. When the parents of nodes change, the depth model
   * "should" change to reflect the changes in branches. The flat model does not
   * need to change, but has a higher likelyhood of collisions.
   * 
   * @param tree
   * @param path
   * @param spatial
   * @param useDepthKeys
   * @return
   */
  public Map<String, UserData> buildTree(Map<String, UserData> tree, String path, Spatial spatial, boolean includeGeometries, boolean useDepthKeys) {
    if (useDepthKeys) {
      path = path + KEY_SEPERATOR + spatial.getName();
    } else {
      path = spatial.getName();
    }
    if (tree.containsKey(path)) {
      UserData s = tree.get(path);
      log.error("buildTree collision {}", path);
    }

    // only interested in nodes, since nodes "can" have user data...
    // Geometries cannot or (should not?)
    if (!includeGeometries && (spatial instanceof Geometry)) {
      return tree;
    }

    // putting both nodes & geometries on the tree
    tree.put(path, spatial.getUserData("data"));

    if (spatial instanceof Node) {
      List<Spatial> children = ((Node) spatial).getChildren();
      for (int i = 0; i < children.size(); ++i) {
        buildTree(tree, path, children.get(i), includeGeometries, useDepthKeys);
      }
    }
    return tree;
  }

  public void cameraLookAt(Spatial spatial) {

    // INTERESTING BUG - DO NOT DIRECTLY LOOK AT BECAUSE WHEN WE PUT COMMANDS IN
    // ORDER
    // ROTATING (to lookAt) IS NOT TRANSITIVE, AND THIS HAPPENS BEFORE ANY
    // PREVIOUS MOVE :P
    // SO IT DOES NOT WORK - solution is to process the lookAt with the JME
    // thread processing
    // all the other moves & rotations !
    // camera.lookAt(spatial.getWorldTranslation(), Vector3f.UNIT_Y);
    addMsg("lookAt", CAMERA, spatial.getName());
  }

  public void cameraLookAt(String name) {
    Spatial s = get(name);
    if (s == null) {
      log.error("cameraLookAt - cannot find {}", name);
      return;
    }
    cameraLookAt(s);
  }

  public void cameraLookAtRoot() {
    cameraLookAt(rootNode);
  }

  // FIXME make a more general Collision check..
  public Geometry checkCollision() {

    // Reset results list.
    CollisionResults results = new CollisionResults();
    // Convert screen click to 3d position
    Vector2f click2d = inputManager.getCursorPosition();
    Vector3f click3d = cameraSettings.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
    Vector3f dir = cameraSettings.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
    // Aim the ray from the clicked spot forwards.
    Ray ray = new Ray(click3d, dir);
    // Collect intersections between ray and all nodes in results list.
    rootNode.collideWith(ray, results);
    // (Print the results so we see what is going on:)
    for (int i = 0; i < results.size(); i++) {
      // (For each “hit”, we know distance, impact point, geometry.)
      float dist = results.getCollision(i).getDistance();
      Vector3f pt = results.getCollision(i).getContactPoint();
      String target = results.getCollision(i).getGeometry().getName();
      System.out.println("Selection #" + i + ": " + target + " at " + pt + ", " + dist + " WU away.");
    }
    // Use the results -- we rotate the selected geometry.
    if (results.size() > 0) {
      // The closest result is the target that the player picked:
      Geometry target = results.getClosestCollision().getGeometry();
      // Here comes the action:
      log.info("you clicked " + target.getName());
      return target;
    }
    return null;
  }

  public void clone(String name, String newName) {

  }

  public Geometry createBoundingBox(Spatial spatial, String color) {
    return util.createBoundingBox(spatial, color);
  }

  public Node createUnitAxis(String name) {
    return util.createUnitAxis(name);
  }

  /**
   * cycles through children at same level
   */
  public void cycle() {

    if (selectedForView == null) {
      Spatial s = rootNode.getChild(0);
      setSelected(s);
    }

    Node parent = selectedForView.getParent();
    if (parent == null) {
      return;
    }

    List<Spatial> siblings = parent.getChildren();

    if (shiftLeftPressed) {
      --selectIndex;
    } else {
      ++selectIndex;
    }

    if (selectIndex > siblings.size() - 1) {
      selectIndex = 0;
    } else if (selectIndex < 0) {
      selectIndex = siblings.size() - 1;
    }

    setSelected(siblings.get(selectIndex));
  }

  // FIXME !!!! enableCoodinateAxes - same s bb including parent if geometry
  public void enableAxes(Spatial spatial, boolean b) {

    /**
     * mmm - may be a bad idea - but may need to figure solution out.. if
     * (spatial instanceof Geometry) { UserData data =
     * jme.getUserData(spatial.getParent()); data.enableCoordinateAxes(b);
     * return; }
     */

    if (spatial.getName().startsWith("_")) {
      log.warn("enableAxes(%s) a meta object not creating/enabling", spatial.getName());
      return;
    }

    // String name = getCoorAxesName(spatial);

    // we need the geometry's parent
    Node parent = spatial.getParent();
    // we need to check to see if this uniquely named Geometry's bb exists ..
    String axesName = getCoorAxesName(spatial); //
    Spatial axis = find(axesName, parent);
    if (axis == null) {
      axis = createUnitAxis(axesName);
    }

    if (spatial instanceof Geometry) {
      parent.attachChild(axis);
    } else {
      // spatial is a node - attach it directly
      ((Node) spatial).attachChild(axis);
    }
    /*
     * if (axis == null) { axis = jme.createUnitAxis();
     * axis.setLocalTranslation(spatial.getWorldTranslation()); << ???
     * axis.setLocalRotation(spatial.getWorldRotation()); ((Node)
     * spatial).attachChild(axis); }
     */
    if (b) {
      axis.setCullHint(CullHint.Never);
    } else {
      axis.setCullHint(CullHint.Always);
    }
  }

  public void enableBoundingBox(Spatial spatial, boolean b) {
    enableBoundingBox(spatial, b, null);
  }

  public void enableBoundingBox(Spatial spatial, boolean b, String color) {
    if (spatial == null) {
      log.error("enableBoundingBox(null) - spatial cannot be null");
      return;
    }

    String name = spatial.getName();

    if (name.startsWith("_")) {
      log.warn("enableBoundingBox(%s) begins with \"_\" is a meta node - will not create new bounding box", name);
      // might not be desirable to simply return - might need to "turn off" an
      // existing bounding box
      return;
    }

    if (color == null) {
      color = Jme3Util.defaultColor;
    }

    // we need the geometry's parent
    Node parent = spatial.getParent();
    // we need to check to see if this uniquely named Geometry's bb exists ..
    String geoBbName = getBbName(spatial); //
    Spatial bb = find(geoBbName, parent);
    if (bb == null) {
      bb = createBoundingBox(spatial, color);
      if (bb == null) {
        log.info("bb for {} could not be created", spatial.getName());
        return;
      }
    }
    // now we have the bb

    // BB is a "new" object - and you can't add nodes to a Geometry,
    // so current strategy is to grab the Geometry's parent and add
    // a name "unique" BB for that Geometry

    if (spatial instanceof Geometry) {
      parent.attachChild(bb);
    } else {
      // spatial is a node - attach it directly
      ((Node) spatial).attachChild(bb);
    }

    // FIXME !!! - so it turns out scale is correct if NOT attached to the
    // node/tree system which has been scaled :P
    // the following gives an accurately sized bounding box - BUT it will not
    // move with the node in question :(
    // rootNode.attachChild(bb);

    if (b) {
      bb.setCullHint(CullHint.Never);
    } else {
      bb.setCullHint(CullHint.Always);
    }
  }

  public void enableBoundingBox(String name, boolean b) {
    enableBoundingBox(get(name), b, null);
  }

  public void enableFlyCam(boolean b) {
    flyCam.setEnabled(b);
  }

  public void enableFullScreen(boolean fullscreen) {
    this.fullscreen = fullscreen;

    if (fullscreen) {
      GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      displayMode = device.getDisplayMode();
      // DisplayMode[] modes = device.getDisplayModes(); list of possible diplay
      // modes

      // remember last display mode
      displayMode = device.getDisplayMode();

      settings = app.getContext().getSettings();
      log.info("settings {}", settings);
      settings.setTitle(getName());
      settings.setResolution(displayMode.getWidth(), displayMode.getHeight());
      settings.setFrequency(displayMode.getRefreshRate());
      settings.setBitsPerPixel(displayMode.getBitDepth());

      // settings.setFullscreen(device.isFullScreenSupported());
      settings.setFullscreen(fullscreen);
      app.setSettings(settings);
      app.restart();

      // app.restart(); // restart the context to apply changes
    } else {
      settings = app.getContext().getSettings();
      log.info("settings {}", settings);
      /*
       * settings.setFrequency(displayMode.getRefreshRate());
       * settings.setBitsPerPixel(displayMode.getBitDepth());
       */
      settings.setFullscreen(fullscreen);
      settings.setResolution(width, height);
      app.setSettings(settings);
      app.restart();
    }
  }

  // FYI - not done with the JME thread ..
  // TODO - addGrid called from enableFloorGrid
  public void enableGrid(boolean b) {
    Spatial s = find("floor-grid");
    if (s == null) {
      addGrid("floor-grid");
      s = get("floor-grid");
    }
    if (b) {
      s.setCullHint(CullHint.Never);
    } else {
      s.setCullHint(CullHint.Always);
    }
  }

  public Spatial find(String name) {
    return find(name, null);
  }

  /**
   * the all purpose find by name method
   * 
   * @param name
   *          - name of node
   * @param startNode
   *          - the node to start search
   * @return
   */
  public Spatial find(String name, Node startNode) {
    if (name.equals(ROOT)) {
      return rootNode;
    }
    if (startNode == null) {
      startNode = rootNode;
    }

    // FIXME - is this needed ?
    if (nameMappings.containsKey(name)) {
      name = nameMappings.get(name);
    }

    Spatial child = startNode.getChild(name);
    return child;
  }

  public String format(Node node, Integer selected) {
    StringBuilder sb = new StringBuilder();
    List<Spatial> children = node.getChildren();
    sb.append("[");
    for (int i = 0; i < children.size(); ++i) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(node.getChild(i).getName());
    }
    sb.append("]");
    return sb.toString();
  }

  public Spatial get(String name) {
    return get(name, null);
  }

  /**
   * wrapper of "find" which "expects" a spatial back otherwise its an error
   * 
   * @param name
   * @param startNode
   * @return
   */
  public Spatial get(String name, Node startNode) {
    Spatial ret = find(name, startNode);
    if (ret == null) {
      error("get(%s) could not find child", name);
    }
    return ret;
  }

  /**
   * get default axis local rotation in degrees
   * 
   * @param name
   * @return
   */
  public Float getAngle(String name) {
    return getAngle(name, null);
  }

  public Float getAngle(String name, String axis) {
    Spatial s = get(name);
    if (s == null) {
      return null;
    }
    Quaternion q = s.getLocalRotation();
    float[] angles = new float[3];
    q.toAngles(angles);
    UserData data = getUserData(name);
    // default rotation is around Y axis unless specified
    Vector3f rotMask = null;

    if (axis != null) {
      rotMask = util.getUnitVector(axis); // Vector3f.UNIT_Y;
    } else {
      rotMask = data.rotationMask;
    }

    int axisIndex = Jme3Util.getIndexFromUnitVector(rotMask);
    float rawAngle = angles[axisIndex] * 180 / FastMath.PI;

    float result = rawAngle;
    if (data.mapper != null) {
      result = data.mapper.calcInput((double)rawAngle).floatValue();
    }
    return result;
  }

  public Jme3App getApp() {
    return app;
  }

  public AssetManager getAssetManager() {
    return assetManager;
  }

  public String getBbName(Spatial spatial) {
    if (spatial.getName().startsWith("_")) {
      return null;
    }
    String geoBbName = String.format("_bb-%s-%s", getType(spatial), spatial.getName());
    return geoBbName;
  }

  public String getCoorAxesName(Spatial spatial) {
    if (spatial.getName().startsWith("_")) {
      return null;
    }
    String geoBbName = String.format("_axis-%s-%s", getType(spatial), spatial.getName());
    return geoBbName;
  }

  private String getExt(String name) {
    int pos = name.lastIndexOf(".");
    String ext = null;
    if (pos != -1) {
      ext = name.substring(pos + 1).toLowerCase();
    }
    return ext;
  }

  public Geometry getGeometry(String name) {
    return getGeometry(name, null);
  }

  public Geometry getGeometry(String name, Node startNode) {
    Spatial spatial = get(name, startNode);
    if (spatial instanceof Geometry) {
      return (Geometry) spatial;
    } else {
      log.error("could not find Geometry {}", name);
      return null;
    }
  }

  public Queue<Jme3Msg> getjmeMsgQueue() {
    return jme3MsgQueue;
  }

  public String getKeyPath(Spatial spatial) {
    if (spatial == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(spatial.getName());
    Node p = spatial.getParent();
    while (p != null) {
      sb.insert(0, String.format("%s%s", p.getName(), KEY_SEPERATOR));
      p = p.getParent();
    }
    return sb.toString();
  }

  private String getNameNoExt(String name) {
    int pos = name.lastIndexOf(".");
    String nameNoExt = name;
    if (pos != -1) {
      nameNoExt = name.substring(0, pos);
    }
    return nameNoExt;
  }

  public Node getNode(Spatial spatial) {
    if (spatial instanceof Geometry) {
      return spatial.getParent();
    }
    return (Node) spatial;
  }

  public Node getNode(String name) {
    return getNode(name, null);
  }

  public Node getNode(String name, Node startNode) {
    Spatial spatial = get(name, startNode);
    if (spatial instanceof Node) {
      return (Node) spatial;
    } else {
      log.error("could not find Node {}", name);
      return null;
    }
  }

  public Spatial getRootChild(Spatial spatial) {
    if (spatial == null) {
      log.error("spatial is null");
      return null;
    }
    Spatial c = spatial;
    Spatial p = c.getParent();

    if (spatial == rootNode) {
      return null;
    }

    while (p != null && p != rootNode) {
      c = p;
      p = c.getParent();
    }

    if (p != null) {
      return c;
    }
    return spatial;
  }

  public Node getRootNode() {
    return rootNode;
  }

  public Spatial getSelected() {
    return selectedForView;
  }

  public AppSettings getSettings() {
    return settings;
  }

  public Spatial getTopNode(Spatial spatial) {
    if (spatial == null) {
      return null;
    }
    Spatial top = spatial;
    while (top.getParent() != null) {
      top = top.getParent();
    }
    return top;
  }

  public String getType(Spatial spatial) {
    if (spatial instanceof Node) {
      return "n";
    } else {
      return "g";
    }
  }

  // TODO - possibly Geometries
  // Unique Naming and map/index
  public UserData getUserData(Node node) {
    UserData data = node.getUserData("data");
    if (data == null) {
      data = new UserData(this, node);
      // FIXME - add map/index
      // getAncestorKey(x) + rootKey if its not root = key
    }
    return data;
  }

  /**
   * The workhorse - where everyone "searches" for the user data they need. It
   * works agains a flat or depth key'd tree. If the node is found but the user
   * data has not been created, it creates it and assigns the references... if
   * the node cannot be found, it returns null
   * 
   * @param path
   *          - full path for a depth tree, name for a flat map
   * @return
   */
  public UserData getUserData(String path /* , boolean useDepth */) {

    Spatial spatial = get(path);

    if (spatial == null) {
      error("geteUserData %s cannot be found", path);
      return null;
    }

    if (spatial instanceof Geometry) {
      error("geteUserData %s found but is Geometry not Node", path);
      return null;
    }

    UserData userData = spatial.getUserData("data");
    if (userData == null) {
      userData = new UserData(this, spatial);
    }
    return userData;
  }

  public void hide(String name) {
    setVisible(name, true);
  }

  public void initPointCloud(PointCloud pc) {

    Point3df[] points = pc.getData();
    Vector3f[] lineVerticies = new Vector3f[points.length];

    for (int i = 0; i < points.length; ++i) {
      Point3df p = points[i];
      lineVerticies[i] = new Vector3f(p.x, p.y, p.z);
    }

    pointCloudBuffer = BufferUtils.createFloatBuffer(lineVerticies);

    // pointCloudMesh.setMode(Mesh.Mode.TriangleFan);
    pointCloudMesh.setMode(Mesh.Mode.Points);
    // pointCloudMesh.setMode(Mesh.Mode.Lines);
    // pointCloudMesh.setMode(Mesh.Mode.Triangles);

    // https://hub.jmonkeyengine.org/t/how-to-render-a-3d-point-cloud/27341/11
    pointCloudMesh.setBuffer(VertexBuffer.Type.Position, 3, pointCloudBuffer);
    pointCloudMesh.setBuffer(VertexBuffer.Type.Color, 4, pc.getColors());
    pointCloudMesh.updateBound();
    pointCloudMesh.updateCounts();
    // pointCloudMesh.setPointSize(0.0003);

    Geometry geo = new Geometry("line", pointCloudMesh);
    pointCloudMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    // pointCloudMat = new Material(assetManager,
    // "Common/MatDefs/Misc/Particle.j3md");

    pointCloudMat.setColor("Color", ColorRGBA.Green);
    pointCloudMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
    // pointCloudMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Front);
    pointCloudMat.setBoolean("VertexColor", false); // important for points !!
    // pointCloudMat.setBoolean("VertexColor", false); // important for points
    // !!
    geo.setMaterial(pointCloudMat);

    rootNode.attachChild(geo);
  }

  // FIXME - more parameters - location & rotation (new function "move")
  // FIXME - scale should not be in this - scale as one of 3 methods rotate !!!!
  // translate
  // TODO - must be re-entrant - perhaps even on a schedule ?
  // TODO - removeNode
  public void load(String inFileName) {
    log.info("load({})", inFileName);
    // UserData o = null;
    try {

      if (inFileName == null) {
        error("file name cannot be null");
        return;
      }

      File file = getFile(inFileName);

      if (!file.exists()) {
        error(String.format("file %s does not exits", inFileName));
        return;
      }

      String filename = file.getName();
      String ext = getExt(filename);
      String simpleName = getNameNoExt(filename);

      if (!ext.equals("json")) {

        Spatial spatial = assetManager.loadModel(filename);
        spatial.setName(simpleName);

        Node node = null;

        if (spatial instanceof Node) {
          node = (Node) spatial;
        } else {
          node = new Node(spatial.getName());
          node.attachChild(spatial);
        }

        rootNode.attachChild(node);

      } else {

        String json = FileIO.toString(filename);
        Jme3Msg[] msgs = CodecUtils.fromJson(json, Jme3Msg[].class);
        log.info("adding {} msgs", msgs.length);
        for (Jme3Msg msg : msgs) {
          jme3MsgQueue.add(msg);
        }

        // now for the json meta data ....
        /*
         * String json = FileIO.toString(inFileName); Map<String, Object> list =
         * CodecUtils.fromJson(json, nodes.getClass()); for (String name :
         * list.keySet()) { String nodePart = CodecUtils.toJson(list.get(name));
         * UserData node = CodecUtils.fromJson(nodePart, UserData.class); //
         * get/create transient parts //
         * node.setService(Runtime.getService(name)); // node.setJme(this); //
         * putN // nodes.put(node.getName(), node); }
         */
      }

    } catch (Exception e) {
      error(e);
    }
  }

  public Spatial loadModel(String assetPath) {
    return assetManager.loadModel(assetPath);
  }

  public void loadModels() {
    // load the root data dir
    loadModels(modelsDir);
    loadNodes(modelsDir);
  }

  public void loadModels(String dirPath) {
    log.info("loading models from {}", dirPath);
    File dir = new File(dirPath);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    if (!dir.isDirectory()) {
      error("%s is not a directory", dirPath);
      return;
    }
    assetManager.registerLocator(dirPath, FileLocator.class);
    // get list of files in dir ..
    File[] files = dir.listFiles();

    // scan for all non json files first ...
    // initially set them invisible ...
    for (File f : files) {
      if (!f.isDirectory()) { // && !"json".equals(getExt(f.getName()))) {
        load(f.getAbsolutePath());
      }
    }

    // process structure json files ..

    // breadth first search ...
    for (File f : files) {
      if (f.isDirectory()) {
        loadModels(f.getAbsolutePath());
      }
    }
  }

  /**
   * load a node with all potential children
   * 
   * @param parentDirPath
   */
  public void loadNode(String parentDirPath) {
    File parentFile = new File(parentDirPath);
    if (!parentFile.isDirectory()) {
      // parent is not a directory ...
      // we are done here ..
      return;
    }

    String parentName = parentFile.getName();

    File[] files = parentFile.listFiles();
    // depth first search - process all children first
    // to build the tree
    for (File f : files) {
      if (f.isDirectory()) {
        loadNode(f.getAbsolutePath());
      }
    }

    Node parentNode = getNode(parentName);
    // parent is a dir - we have processed our children - now we process
    // the parent "if" we don't already have a reference to a node with the same
    // name
    if (parentNode == null) {
      putNode(parentName);
      parentNode = putNode(parentName);

      for (File f : files) {
        String childname = getNameNoExt(f.getName());
        if (getNode(childname) == null) {
          log.error("loadNode {} can not attach child to {} not found in nodes", parentDirPath, childname);
        } else {
          parentNode.attachChild(getNode(childname));
        }
      }
    } else {
      // FIXME - it "may" already contain the parent name - but also "may" not
      // have all sub-children attached
      // possibly implement - attaching children
    }

    // index(parentNode);
    // saveNodes();
  }

  /**
   * based on a directory structure - add missing nodes and bindings top node
   * will be bound to root
   * 
   * @param dirPath
   */
  public void loadNodes(String dirPath) {
    File dir = new File(dirPath);
    if (!dir.isDirectory()) {
      error("%s is not a directory", dirPath);
      return;
    }
    // get list of files in dir ..
    File[] files = dir.listFiles();

    // scan for all non json files first ...
    // initially set them invisible ...
    for (File f : files) {
      if (f.isDirectory()) {
        loadNode(f.getAbsolutePath());
      }
    }
  }

  public void lookAt(String viewer, String viewee) {
    addMsg("lookAt", viewer, viewee);
  }

  // FIXME - implement - relative move
  public void move(String name, double x, double y) {
    addMsg("move", name, x, y);
  }

  public void moveTo(String name, double x, double y, double z) {
    setTranslation(name, x, y, z);
  }

  @Override
  public void onAction(String name, boolean keyPressed, float tpf) {
    log.info("onAction {} {} {}", name, keyPressed, tpf);

    if (name.equals("mouse-click-right")) {
      mouseRightPressed = keyPressed;
      if (mouseRightPressed) {
        Geometry target = checkCollision();
        setSelected(target);
      }
    }

    if ("full-screen".equals(name)) {
      enableFullScreen(true);
    } else if ("menu".equals(name)) {
      menu.setEnabled(true);
    } else if ("select-root".equals(name)) {
      setSelected(rootNode);
    } else if (CAMERA.equals(name)) {
      setSelected(CAMERA);
    } else if ("exit-full-screen".equals(name)) {
      enableFullScreen(false);
    } else if ("cycle".equals(name) && keyPressed) {
      cycle();
    } else if (name.equals("shift-left")) {
      shiftLeftPressed = keyPressed;
    } else if (name.equals("ctrl-left")) {
      ctrlLeftPressed = keyPressed;
    } else if (name.equals("alt-left")) {
      altLeftPressed = keyPressed;
    } else if ("export".equals(name) && keyPressed) {
      saveSpatial(selectedForView.getName());
    } else if ("mouse-click-left".equals(name)) {
      mouseLeftPressed = keyPressed;
    } else {
      warn("%s - key %b %f not found", name, keyPressed, tpf);
    }
  }

  /**
   * onAnalog
   * 
   * @param name
   * @param keyPressed
   * @param tpf
   */
  public void onAnalog(String name, float keyPressed, float tpf) {
    // log.info("onAnalog [{} {} {}]", name, keyPressed, tpf);

    if (selectedForMovement == null) {
      selectedForMovement = camera;// FIXME "new" selectedMove vs selected
    }

    // wheelmouse zoom (done)
    // alt+ctrl+lmb - zoom <br> (done)
    // alt+lmb - rotate<br> (done)
    // alt+shft+lmb - pan (done)
    // rotate around selection -
    // https://www.youtube.com/watch?v=IVZPm9HAMD4&feature=youtu.be
    // wrap text of breadcrumbs
    // draggable - resize for menu - what you set is how it stays
    // when menu active - inputs(hotkey when non-menu) should be deactive

    // FIXME - do jme.rotateTo or "new" jme.rotate for all these input driven
    // controls

    // ROTATE
    if (mouseLeftPressed && altLeftPressed && !shiftLeftPressed) {
      if (name.equals("mouse-axis-x")) {
        selectedForMovement.rotate(0, -keyPressed, 0);
      } else if (name.equals("mouse-axis-x-negative")) {
        selectedForMovement.rotate(0, keyPressed, 0);
      } else if (name.equals("mouse-axis-y")) {
        selectedForMovement.rotate(-keyPressed, 0, 0);
      } else if (name.equals("mouse-axis-y-negative")) {
        selectedForMovement.rotate(keyPressed, 0, 0);
      }
    }

    // PAN
    if (mouseLeftPressed && altLeftPressed && shiftLeftPressed) {
      if (name.equals("mouse-axis-x")) {
        selectedForMovement.move(keyPressed * 3, 0, 0);
      } else if (name.equals("mouse-axis-x-negative")) {
        selectedForMovement.move(-keyPressed * 3, 0, 0);
      } else if (name.equals("mouse-axis-y")) {
        selectedForMovement.move(0, keyPressed * 3, 0);
      } else if (name.equals("mouse-axis-y-negative")) {
        selectedForMovement.move(0, -keyPressed * 3, 0);
      }
    }

    // ZOOM
    if (mouseLeftPressed && altLeftPressed && ctrlLeftPressed) {

      // FIXME - zoom where cursor is :P - it becomes a rotate and zoom
      log.info("zoom - cursor is currently {}", inputManager.getCursorPosition());

      if (name.equals("mouse-axis-y")) {
        selectedForMovement.move(0, 0, keyPressed * 10);
      } else if (name.equals("mouse-axis-y-negative")) {
        selectedForMovement.move(0, 0, -keyPressed * 10);
      }
    }

    if (name.equals("mouse-wheel-up") || name.equals("forward")) {
      // selected.setLocalScale(selected.getLocalScale().mult(1.0f));
      selectedForMovement.move(0, 0, keyPressed * -1);
    } else if (name.equals("mouse-wheel-down") || name.equals("backward")) {
      // selected.setLocalScale(selected.getLocalScale().mult(1.0f));
      selectedForMovement.move(0, 0, keyPressed * 1);
    }
  }

  /**
   * A method to accept Computer Vision data (from OpenCV or BoofCv) and to
   * appropriately delegate it out to more specific methods
   * 
   * @param data
   */
  public void onCvData(CvData data) {
    // onPointCloud(data.getPointCloud()); FIXME - brittle and not correct
    // FIXME - do something interesting ... :)
  }

  public void onPointCloud(PointCloud pc) {

    if (pc == null) {
      return;
    }
    // pointCloudMat.setBoolean("VertexColor", false);
    // pointCloudMesh.setPointSize(0.01f);

    if (pointCloudBuffer == null) {
      initPointCloud(pc);
      // addBox("box-1");
    }

    pointCloudBuffer.rewind();
    Point3df[] points = pc.getData();

    for (int i = 0; i < points.length; ++i) {
      Point3df p = points[i];
      // log.info("p {}", p);
      // pointCloudBuffer.put(480 - p.y);
      // pointCloudBuffer.put(640 - p.x);
      pointCloudBuffer.put(p.x);
      pointCloudBuffer.put(p.y);
      pointCloudBuffer.put(p.z);
      // lineVerticies[index] = new Vector3f(480 - p.y, 640 - p.x, p.z);
    }

    pointCloudMesh.setBuffer(VertexBuffer.Type.Position, 3, pointCloudBuffer);
    pointCloudMesh.setBuffer(VertexBuffer.Type.Color, 4, pc.getColors());
  }

  // auto Register
  public void onRegistered(Registration registration) throws Exception {
    // new service - see if we can virtualize it
    log.info("{}.onRegistered({})", getName(), registration);
    if (registration.getName().contentEquals("i01.head.jaw")) {
      log.info("here");
    }
    if (autoAttach) {
      attach(registration.getFullName());
    }
  }

  public void onRegistered(Servo servo) throws Exception {
    attach(servo);
  }

  public Node putNode(String name) {
    Node check = getNode(name);
    if (check != null) {
      return check;
    }
    Node n = new Node(name);
    rootNode.attachChild(n);
    return n;
  }

  public void putText(Spatial spatial, int x, int y) {
    Vector3f xyz = spatial.getWorldTranslation();
    Quaternion q = spatial.getLocalRotation();
    float[] angles = new float[3]; // yaw, roll, pitch
    q.toAngles(angles);

    boolean isNode = (spatial instanceof Node);

    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s-%s\n", (isNode) ? "node" : "geom", spatial.getName()));
    sb.append(String.format("x:%.3f y:%.3f z:%.3f\n", xyz.x, xyz.y, xyz.z));
    sb.append(String.format("yaw:%.2f roll:%.2f pitch:%.2f\n", angles[0] * 180 / FastMath.PI, angles[1] * 180 / FastMath.PI, angles[2] * 180 / FastMath.PI));

    if (isNode) {
      sb.append(format((Node) spatial, 0));
    }

    putText(sb.toString(), 10, 10);
  }

  public void putText(String text, int x, int y) {
    putText(text, x, y, null, null);
  }

  // put 2d text into a 3d scene graph
  public void putText(String text, int x, int y, int z) {

    Node n;
    Node n2;

    Quad q = new Quad(2, 2);
    Geometry g = new Geometry("Quad", q);
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.Blue);
    g.setMaterial(mat);

    Quad q2 = new Quad(1, 1);
    Geometry g3 = new Geometry("Quad2", q2);
    Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat2.setColor("Color", ColorRGBA.Yellow);
    g3.setMaterial(mat2);
    // g3.setLocalTranslation(.5f, .5f, .01f);

    // Box b = new Box(.25f, .5f, .25f);
    Box b = new Box(1.0f, 1.0f, 1.0f);
    Geometry g2 = new Geometry("Box", b);
    // g2.setLocalTranslation(0, 0, 3);
    g2.setMaterial(mat);

    BitmapFont font = assetManager.loadFont("Common/Default.fnt");
    BitmapText bmText = new BitmapText(font, false);

    bmText.setSize(30);
    bmText.setText("Billboard Data");
    bmText.setQueueBucket(Bucket.Transparent);
    bmText.setColor(ColorRGBA.White);

    Node bb = new Node("billboard");

    BillboardControl control = new BillboardControl();
    control.setAlignment(BillboardControl.Alignment.Screen);

    bb.addControl(control);
    bb.attachChild(bmText);
    bb.attachChild(g);
    bb.attachChild(g3);

    n = new Node("parent");
    n.attachChild(g2);
    n.attachChild(bb);
    rootNode.attachChild(n);

    n2 = new Node("parentParent");
    n2.setLocalTranslation(Vector3f.UNIT_X.mult(5));
    n2.attachChild(n);

    rootNode.attachChild(n2);
  }

  public void putText(String text, int x, int y, String color) {
    putText(text, x, y, color, null);
  }

  /**
   * put text on the guiNode HUD display for jmonkey FIXME - do the same logic
   * in OpenCV overlay !
   * 
   * @param text
   * @param x
   * @param y
   */
  public void putText(String text, int x, int y, String color, Integer size) {
    HudText hud = null;

    if (color == null) {
      color = fontColor;
    }

    if (size == null) {
      size = fontSize;
    }

    String key = String.format("%d-%d", x, y);
    if (guiText.containsKey(key)) {
      hud = guiText.get(key);
      hud.setText(text, color, size);
    } else {
      hud = new HudText(this, text, x, y);
      hud.setText(text, color, size);
      guiText.put(key, hud);
      app.getGuiNode().attachChild(hud.getNode());
    }
  }

  public void rename(String name, String newName) {
    Spatial data = get(name);
    if (data == null) {
      error("rename(%s, %s) could not find %s", name, newName, name);
      return;
    }
    data.setName(newName);
  }

  /**
   * rotate on the "default" axis to a location without using speed
   * 
   * @param name
   * @param degrees
   */
  public void rotateTo(String name, double degrees) {
    addMsg("rotateTo", name, null, degrees);
  }

  /**
   * rotate on the "default" axis using speed
   * 
   * @param name
   * @param degrees
   * @param speed
   */
  public void rotateTo(String name, double degrees, double speed) {
    interpolator.addAnimation("rotateTo", name, null, degrees, speed);
  }

  /**
   * instant rotation on an particular axis
   * 
   * @param name
   * @param axis
   * @param degrees
   */
  public void rotateOnAxis(String name, String axis, double degrees) {
    addMsg("rotateTo", name, axis, degrees);
  }

  /**
   * incremental movement on an axis with a speed
   * 
   * @param name
   * @param axis
   * @param degrees
   */
  public void rotateOnAxis(String name, String axis, double degrees, double speed) {
    interpolator.addAnimation("rotateTo", name, axis, degrees, speed);
  }

  // this just saves keys !!!
  public void saveKeys(Spatial toSave) {
    try {
      String filename = FileIO.cleanFileName(toSave.getName()) + ".txt";
      TreeMap<String, UserData> tree = new TreeMap<String, UserData>();
      buildTree(tree, "", toSave, false, true);
      // String ret = CodecUtils.toJson(tree);
      FileOutputStream fos = new FileOutputStream(filename);
      for (String key : tree.keySet()) {
        String type = (tree.get(key) != null && tree.get(key).getSpatial() != null && tree.get(key).getSpatial() instanceof Node) ? " (Node)" : " (Geometry)";
        fos.write(String.format("%s\n", key + type).getBytes());
      }
      fos.close();
    } catch (Exception e) {
      error(e);
    }
  }

  public boolean saveNodes() {
    return saveSpatial(rootNode, null);
  }

  public boolean saveSpatial(Spatial spatial) {
    return saveSpatial(spatial, null);
  }

  // FIXME - fix name - because it can save a Geometry too
  public boolean saveSpatial(Spatial spatial, String filename) {
    try {

      if (spatial == null) {
        error("cannot save null spatial");
        return false;
      }

      String name = spatial.getName();

      if (filename == null) {
        filename = name + ".j3o";
      }

      filename = FileIO.cleanFileName(filename);
      BinaryExporter exporter = BinaryExporter.getInstance();
      FileOutputStream out = new FileOutputStream(filename);
      exporter.save(spatial, out);
      out.close();

      /*
       * worthless...
       * 
       * out = new FileOutputStream(name + ".xml"); XMLExporter xmlExporter =
       * XMLExporter.getInstance(); xmlExporter.save(spatial, out); out.close();
       */

      return true;
    } catch (Exception e) {
      log.error("exporter.save threw", e);
    }
    return false;
  }

  public boolean saveSpatial(String name) {
    Spatial spatial = get(name);
    return saveSpatial(spatial, spatial.getName());
  }

  // FIXME - base64 encoding of j3o file - "all in one file" gltf instead ???
  public boolean saveToJson(String jsonPath) {
    try {
      String json = CodecUtils.toJson(buildTree());
      FileIO.toFile(jsonPath, json.getBytes());
      return true;
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  // TODO - need to make thread safe ? JME thread ?
  // turn it into a jme msg - put it on the update queue ?
  public void scale(String name, double scale) {
    addMsg("scale", name, scale);
  }

  public List<Spatial> search(String text) {
    return search(text, null, null, null);
  }

  public List<Spatial> search(String text, Node beginNode, Boolean exactMatch, Boolean includeGeometries) {
    if (beginNode == null) {
      beginNode = rootNode;
    }
    if (exactMatch == null) {
      exactMatch = false;
    }
    if (includeGeometries == null) {
      includeGeometries = true;
    }
    Search search = new Search(text, exactMatch, includeGeometries);
    beginNode.breadthFirstTraversal(search);
    return search.getResults();
  }

  public void setDisplayFps(boolean b) {
    app.setDisplayFps(b);
  }

  public void setDisplayStatView(boolean b) {
    app.setDisplayStatView(b);
  }

  public void setFontColor(String color) {
    fontColor = color;
  }

  public void setFontSize(int size) {
    fontSize = size;
  }

  public Mapper setMapper(String name, int minx, int maxx, int miny, int maxy) {
    UserData node = getUserData(name);
    if (node == null) {
      error("setMapper %s does not exist", name);
      return null;
    }
    node.mapper = new MapperLinear(minx, maxx, miny, maxy);
    return node.mapper;
  }

  public void setRotation(String name, String rotation) {
    UserData o = getUserData(name);
    if (o == null) {
      error("setRotation %s could not be found", name);
      return;
    }
    // WRONG !!!! - getLocalUnitVector
    // o.rotationMask = util.getUnitVector(rotation);
    o.rotationMask = util.getLocalUnitVector(o.getSpatial(), rotation);
  }

  public void setSelected(Spatial newSelected) {

    // turn off old
    if (selectedForView != null) {
      enableBoundingBox(selectedForView, false);
      enableAxes(selectedForView, false);
    }

    // set selected
    selectedForView = newSelected;

    // send the movement utility info on the current selected item & current
    // view
    // so that it can update the view with changes on the item
    // TODO - optimize for when there is no view
    util.setSelectedForView(menu, selectedForView);

    // display in menu
    menu.putText(newSelected);

    // turn on new
    if (newSelected != null) {
      enableBoundingBox(newSelected, true);
      enableAxes(newSelected, true);
    }
  }

  public void setSelected(String name) {
    Spatial s = get(name);
    if (s == null) {
      log.error("setSelected {} is null", name);
      return;
    }
    setSelected(s);
  }

  public void setVisible(boolean b) {
    if (selectedForView != null) {
      if (b) {
        selectedForView.setCullHint(Spatial.CullHint.Inherit);
      } else {
        selectedForView.setCullHint(Spatial.CullHint.Always);
      }
    }
  }

  public void setVisible(String name, boolean b) {
    Spatial s = get(name);
    if (b) {
      s.setCullHint(CullHint.Never);
    } else {
      s.setCullHint(CullHint.Always);
    }
  }

  public void show(String name) {
    setVisible(name, false);
  }

  public void simpleInitApp() {

    stateManager = app.getStateManager();
    // GuiGlobals.initialize(app);
    // Load the 'glass' style
    // BaseStyles.loadGlassStyle();
    // Set 'glass' as the default style when not specified
    // GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

    setDisplayFps(false);

    setDisplayStatView(false);

    // guiNode = app.getGuiNode();

    // wtf - assetManager == null - another race condition ?!?!?
    // after start - these are initialized as "default"
    assetManager = app.getAssetManager();
    inputManager = app.getInputManager();
    // stateManager.attach(state);

    // app = jme.getApp();
    guiNode = app.getGuiNode();
    // Initialize the globals access so that the default
    // components can find what they need.
    GuiGlobals.initialize(app);
    // Load the 'glass' style
    BaseStyles.loadGlassStyle();
    // Set 'glass' as the default style when not specified
    GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

    // disable flycam we are going to use our
    // own camera
    flyCam = app.getFlyByCamera();
    if (flyCam != null) {
      flyCam.setEnabled(false);
    }

    cameraSettings = app.getCamera();
    rootNode = app.getRootNode();
    rootNode.setName(ROOT);
    rootNode.attachChild(camera);

    viewPort = app.getViewPort();
    // Setting the direction to Spatial to camera, this means the camera will
    // copy the movements of the Node
    camNode = new CameraNode("cam", cameraSettings);
    camNode.setControlDir(ControlDirection.SpatialToCamera);
    // camNode.setControlDir(ControlDirection.CameraToSpatial);
    // rootNode.attachChild(camNode);
    // camNode.attachChild(child)
    // camera.setLocation(new Vector3f(0, 1, -1));
    // camNode.setLocalTranslation(-1, 1, -1);
    // camNode.setLocalTranslation(new Vector3f(1f, 1f, 1f));
    // camera.setLocalTranslation(-1, 1, -1);
    camera.attachChild(camNode);
    // camera.move(0, 1, 2);
    // camera.lookAt(rootNode.getLocalTranslation(), Vector3f.UNIT_Y);
    // camNode.lookAt(rootNode.getLocalTranslation(), Vector3f.UNIT_Y);
    // rootNode.attachChild(camNode);
    // rootNode.attachChild(cam);

    // personNode.attachChild(camNode);
    // Screen screen = nifty.getCurrentScreen();

    // loadNiftyGui();

    // cam.setFrustum(0, 1000, 0, 0, 0, 0);
    // cam.setFrustumNear(1.0f);

    inputManager.setCursorVisible(true);

    // camNode.setLocalTranslation(0, 0, 2f);
    // camera.setLocation(new Vector3f(0f, 0f, 2f));
    // cam.setLocation(new Vector3f(0f, 0f, 0f));
    // cam.setLocation(new Vector3f(0f, 0f, 900f));
    // cam.setLocation(new Vector3f(0f, 0f, 12f));
    // cam.setClipPlan);
    new File(getDataDir()).mkdirs();
    new File(getResourceDir()).mkdirs();

    assetManager.registerLocator("./", FileLocator.class);
    assetManager.registerLocator(getDataDir(), FileLocator.class);
    assetManager.registerLocator(assetsDir, FileLocator.class);
    assetManager.registerLocator(getResourceDir(), FileLocator.class);

    // FIXME - should be moved under ./data/JMonkeyEngine/
    //assetManager.registerLocator("InMoov/jm3/assets", FileLocator.class);
    assetManager.registerLocator(getDataDir(), FileLocator.class);
    assetManager.registerLoader(BlenderLoader.class, "blend");

    /**
     * <pre>
     * Physics related bulletAppState = new BulletAppState();
     * bulletAppState.setEnabled(true); stateManager.attach(bulletAppState);
     * PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager,
     * bulletAppState.getPhysicsSpace()); bulletAppState.setDebugEnabled(true);
     */

    // what inputs will jme service handle ?

    /**
     * <pre>
     * LEFT       A and left arrow 
     * RIGHT      D and right arrow
     * UP         W and up arrow
     * DOWN       S and down arrow
     * ZOOM IN    J 
     * ZOOM OUT   K
     * </pre>
     */

    // wheelmouse zoom (check)
    // alt+ctrl+lmb - zoom <br>
    // alt+lmb - rotate<br>
    // alt+shft+lmb - pan
    // rotate around selection -
    // https://www.youtube.com/watch?v=IVZPm9HAMD4&feature=youtu.be
    // wrap text of breadcrumbs
    // draggable - resize for menu - what you set is how it stays
    // when menu active - inputs(hotkey when non-menu) should be deactive

    inputManager.addMapping("mouse-click-left", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(this, "mouse-click-left");

    inputManager.addMapping("mouse-click-right", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(this, "mouse-click-right");

    inputManager.addMapping("mouse-wheel-up", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
    inputManager.addListener(analog, "mouse-wheel-up");
    inputManager.addMapping("mouse-wheel-down", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
    inputManager.addListener(analog, "mouse-wheel-down");

    inputManager.addMapping("mouse-axis-x", new MouseAxisTrigger(MouseInput.AXIS_X, true));
    inputManager.addListener(analog, "mouse-axis-x");

    inputManager.addMapping("mouse-axis-x-negative", new MouseAxisTrigger(MouseInput.AXIS_X, false));
    inputManager.addListener(analog, "mouse-axis-x-negative");

    inputManager.addMapping("mouse-axis-y", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
    inputManager.addListener(analog, "mouse-axis-y");

    inputManager.addMapping("mouse-axis-y-negative", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
    inputManager.addListener(analog, "mouse-axis-y-negative");

    inputManager.addMapping("select-root", new KeyTrigger(KeyInput.KEY_R));
    inputManager.addListener(this, "select-root");

    inputManager.addMapping(CAMERA, new KeyTrigger(KeyInput.KEY_C));
    inputManager.addListener(this, CAMERA);

    inputManager.addMapping("menu", new KeyTrigger(KeyInput.KEY_M));
    inputManager.addListener(this, "menu");
    inputManager.addMapping("full-screen", new KeyTrigger(KeyInput.KEY_F));
    inputManager.addListener(this, "full-screen");
    inputManager.addMapping("exit-full-screen", new KeyTrigger(KeyInput.KEY_G));
    inputManager.addListener(this, "exit-full-screen");
    inputManager.addMapping("cycle", new KeyTrigger(KeyInput.KEY_TAB));
    inputManager.addListener(this, "cycle");
    inputManager.addMapping("shift-left", new KeyTrigger(KeyInput.KEY_LSHIFT));
    inputManager.addListener(this, "shift-left");
    inputManager.addMapping("ctrl-left", new KeyTrigger(KeyInput.KEY_LCONTROL));
    inputManager.addListener(this, "ctrl-left");
    inputManager.addMapping("alt-left", new KeyTrigger(KeyInput.KEY_LMENU));
    inputManager.addListener(this, "alt-left");
    // inputManager.addMapping("mouse-left", new
    // KeyTrigger(MouseInput.BUTTON_LEFT));
    // inputManager.addListener(this, "mouse-left");

    inputManager.addMapping("export", new KeyTrigger(KeyInput.KEY_E));
    inputManager.addListener(this, "export");

    viewPort.setBackgroundColor(ColorRGBA.Gray);

    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));

    rootNode.addLight(sun);

    // AmbientLight sun = new AmbientLight();
    rootNode.addLight(sun);
    // rootNode.scale(.5f);
    // rootNode.setLocalTranslation(0, -200, 0);
    rootNode.setLocalTranslation(0, 0, 0);

    menu = app.getMainMenu();// new MainMenuState(this);
    // menu.loadGui();

    // load models in the default directory
    loadModels();
  }

  public void saveMsgs() throws IOException {
    List<Jme3Msg> temp = history;
    history = new ArrayList<Jme3Msg>();
    String data = CodecUtils.toJson(temp);
    FileIO.toFile(String.format("jme3-msg-history-%d.json", System.currentTimeMillis()), data);
  }

  public void simpleUpdate(float tpf) {

    // start the clock on how much time we will take
    startUpdateTs = System.currentTimeMillis();

    for (HudText hudTxt : guiText.values()) {
      hudTxt.update();
    }

    interpolator.generateMoves();

    while (jme3MsgQueue.size() > 0) {
      Jme3Msg msg = null;
      try {

        // TODO - support relative & absolute moves
        msg = jme3MsgQueue.remove();
        if (saveHistory) {
          history.add(msg);
        }
        util.invoke(msg);
      } catch (Exception e) {
        log.error("simpleUpdate failed for {} - targetName", msg, e);
      }
    }

    deltaMs = System.currentTimeMillis() - startUpdateTs;
    sleepMs = 33 - deltaMs;

    if (sleepMs < 0) {
      sleepMs = 0;
    }
    sleep(sleepMs);
  }

  public SimpleApplication start() {
    return start(defaultAppType, defaultAppType);
  }

  // dynamic create of type... TODO fix name start --> create
  synchronized public SimpleApplication start(String appName, String appType) {
    if (Runtime.isHeadless()) {
      log.warn("running in headless mode - will not start jmonkey app");
      return null;
    }

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
      settings.setResolution(width, height);
      // settings.setEmulateMouse(false);
      // settings.setUseJoysticks(false);
      settings.setUseInput(true);
      settings.setAudioRenderer(null);
      app.setSettings(settings);
      app.setShowSettings(false); // resolution bps etc dialog
      app.setPauseOnLostFocus(false);

      // the all important "start" - anyone goofing around with the engine
      // before this is done will
      // will generate error from jmonkey - this should "block"
      app.start();
      Callable<String> callable = new Callable<String>() {
        public String call() throws Exception {
          System.out.println("Asynchronous Callable");
          return "Callable Result";
        }
      };
      Future<String> future = app.enqueue(callable);
      try {
        future.get();

        // default positioning
        moveTo(CAMERA, 0, 3, 6);
        cameraLookAtRoot();
        rotateOnAxis(CAMERA, "x", -20);
        enableGrid(true);

      } catch (Exception e) {
        log.warn("future threw", e);
      }
      return app;
    }
    info("already started app %s", appType);

    return app;
  }

  public void startService() {
    try {
      super.startService();
      // start the jmonkey app - if you want a diferent Jme3App
      // config should be set at before this time
      SimpleApplication app = start();
      if (app == null) {
        log.warn("jmonkey app not starting");
        return;
      }
      // notify me if new services are created
      subscribe("runtime", "registered");

      if (autoAttach) {
        List<ServiceInterface> services = Runtime.getServices();
        for (ServiceInterface si : services) {
          try {
            attach(si);
          } catch (Exception e) {
            error(e);
          }
        }
      }
    } catch (Exception e) {
      log.error("{} startService exploded", getName(), e);
    }
  }

  // FIXME - requirements for "re-start" is everything correctly de-initialized
  // ?
  synchronized public void stop() {
    if (app != null) {
      try {
        // why ?
        app.getRootNode().detachAllChildren();
        app.getGuiNode().detachAllChildren();
        app.stop(true);
        // app.destroy(); not for "us"
        app = null;
      } catch (Exception e) {
        log.error("stopping jmonkey threw", e);
      }
    }
  }

  @Override
  public void stopService() {
    super.stopService();
    try {
      stop();
    } catch (Exception e) {
      log.error("releasing jme3 app threw", e);
    }
  }

  public void toggleVisible() {
    if (Spatial.CullHint.Always == selectedForView.getCullHint()) {
      setVisible(true);
    } else {
      setVisible(false);
    }
  }

  public String toJson(Node node) {
    // get absolute position info
    return CodecUtils.toJson(node);
    // save it out
  }

  public void setTransform(String name, double x, double y, double z, double xRot, double yRot, double zRot) {
    addMsg("setTransform", name, x, y, z, xRot, yRot, zRot);
  }

  public void setTranslation(String name, double x, double y, double z) {
    addMsg("setTranslation", name, x, y, z);
  }

  public void setRotation(String name, double xRot, double yRot, double zRot) {
    addMsg("setRotation", name, xRot, yRot, zRot);
  }

  public Map<String, String[]> getMultiMapped() {
    return multiMapped;
  }

  @Override
  public void onJointAngles(Map<String, Double> angleMap) {
    for (String name : angleMap.keySet()) {
      ServiceInterface si = Runtime.getService(name);
      if (si instanceof Servo) {
        ((Servo) si).moveTo(angleMap.get(name));
      }
    }
  }

  public static void main(String[] args) {
    try {

      // FIXME - fix menu input system - use jme.rotate/rotateTo/move/moveTo
      // etc.
      // FIXME - node/userdata can have a Map<String, String> of
      // reservedRotations from different controllers
      // FIXME - make "load" work ..

      LoggingFactory.init("info");

      Platform.setVirtual(true);
      Runtime.main(new String[] { "--interactive", "--id", "admin" });

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.setPort(8887);
      webgui.autoStartBrowser(false);
      webgui.startService();

      // Runtime.start("gui", "SwingGui");

      // Arduino left = (Arduino) Runtime.start("i01.left", "Arduino");
      // left.connect("COM4");

      Runtime.start("i01.head.jaw", "Servo");
      

      JMonkeyEngine jme = (JMonkeyEngine) Runtime.start("i01.jme", "JMonkeyEngine");
      jme.setRotation("i01.head.jaw", "x");

      boolean done = true;
      if (done) {
        return;
      }

      for (int i = 0; i < 100; ++i) {
        jme.rotateOnAxis("i01.head.jaw", "x", 100);
        jme.rotateOnAxis("i01.head.jaw", "x", 20);
      }

      // FIXME - fix what you have broken but deprecate sc related rotation info
      /*
       * jme.addNode("xRot"); jme.addNode("yRot"); jme.addNode("zRot");
       * jme.setRotation("xRot", "x"); jme.setRotation("yRot", "y");
       * jme.setRotation("zRot", "z");
       * 
       * jme.bind("xRot", CAMERA); jme.bind("yRot", CAMERA); jme.bind("zRot",
       * CAMERA);
       */

      // jme.setTransform(CAMERA, 0, 3, 6, -20, -180, 0);
      // jme.setTransform(CAMERA, 0.217, 2.508, 1.352, 149.630, -15.429,
      // 47.488);

      // Jme3ServoController sc = (Jme3ServoController)
      // jme.getServoController();
      // FIXME WRONG WAY -
      // setting controllers axis - FIXME - do in more general way
      // jme.setRotation sets a "node"
      // this is the "control" "to" the Node so its "per" control - if the
      // control can support getName()
      /*
       * sc.setRotation("xRot", "x"); // <-- not a property of sc? FIXME - NO!
       * add // new node !!! sc.setRotation("yRot", "y"); sc.setRotation("zRot",
       * "z");
       * 
       * // mapping 3 servos to 3 axis of the camera jme.attach("xRot", CAMERA);
       * jme.attach("yRot", CAMERA); jme.attach("zRot", CAMERA);
       */

      // jme.setTransform(CAMERA, 0, 0, 0, 0, 0, 0);

      // works - can reproduce same view, but when asked to rotate about a
      // specific
      // axis - other axis are resetting :(
      // jme.setTransform(CAMERA, 0.217, 2.508, 1.352, 149.630, -15.429,
      // 47.488);

      // Servo xRot = (Servo) Runtime.start("xRot", "Servo");
      // Servo yRot = (Servo) Runtime.start("yRot", "Servo");
      // Servo zRot = (Servo) Runtime.start("zRot", "Servo");

      /*
       * jme.setRotation("i01.leftHand.index", "x");
       * jme.rotateTo("i01.leftHand.index", 20);
       * jme.rotateTo("i01.leftHand.index", 120);
       * jme.rotateTo("i01.leftHand.index", 20);
       * jme.rotateTo("i01.leftHand.index", 120);
       */

      // Runtime.start("i01.mouth", "NaturalReaderSpeech");
      /*
       * InMoov i01 = (InMoov) Runtime.start("i01", "InMoov");
       * i01.startSimulator(); jme = i01.getSimulator();
       */
      jme.rename("VinMoov4", "i01");
      // jme.scale("i01", 0.25f);

      jme.addBox("floor.box.01", 1.0, 1.0, 1.0, "003300", true);
      jme.moveTo("floor.box.01", 3, 0, 0);

      // ik fun
      // i01.setIkPoint(0.05, 0.05, 0.05);
      // i01.setIkPoint(1, 2.5, 0);

      /*
       * Spatial rotate = jme.get("i01.leftArm.rotate");
       * log.info("rotate world {}", rotate.getLocalTranslation());
       * log.info("rotate local {}", rotate.getWorldTranslation()); Spatial
       * rotateFull = jme.get("i01.leftArm.rotate.full");
       * log.info("rotateFull world {}", rotateFull.getLocalTranslation());
       * log.info("rotateFull local {}", rotateFull.getWorldTranslation());
       */

      // jme.bind(child, parent);

      // find missing mapped servos ...
      List<String> servos = Runtime.getServiceNamesFromInterface("ServoControl");
      for (String servo : servos) {
        Spatial spatial = jme.get(servo);
        if (spatial == null) {
          log.error("cannot find {}", servo);
        }
      }

      log.info("here");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void connect(String uri) throws Exception {
    // easy single client support
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("gateway", getName());
    attributes.put("c-type", getSimpleName());
    attributes.put("id", getName() + "-" + Runtime.getInstance().getId() + "-jme");
    String uuid = java.util.UUID.randomUUID().toString();
    attributes.put("uuid", uuid);
    Runtime.getInstance().addConnection(uuid, attributes);
    Runtime.updateRoute(guiId, uuid);
  }

  @Override
  public List<String> getClientIds() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Map<String, Object>> getClients() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void sendRemote(Message msg) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public Object sendBlockingRemote(Message msg, Integer timeout) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isLocal(Message msg) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Message getDefaultMsg(String connId) {
    // TODO Auto-generated method stub
    return null;
  }

  public void setDefaultServoSpeed(Double speed) {
    defaultServoSpeed = speed;
  }
  /*
   * THERE IS ALSO THE OPTION TO LISTEN TO WHATEVER ENCODER THE SERVO HAS
   * 
   * @Override public void onEncoderData(EncoderData data) { String name =
   * data.source;
   * 
   * String axis = rotationMap.get(name);
   * 
   * String[] multi = multiMapped.get(name); if (multi != null) { for (String
   * nodeName : multi) { jme.rotateOnAxis(nodeName, axis, data.value, velocity);
   * } } else { jme.rotateOnAxis(name, axis, servo.getPos(), velocity); }
   * 
   * }
   */

  /**
   * parameter is not an interface to allow it to be remotely invoked with the
   * MethodCache
   */
  @Override
  public void onServoMoveTo(ServoControl servo) {
    String name = servo.getName();
    /*
     * if (!servos.containsKey(name)) { log.error("servoMoveTo({})", servo);
     * return; }
     */
    Double velocity = servo.getSpeed();
    if (velocity == null || velocity == -1) {
      velocity = defaultServoSpeed;
    }

    // String axis = rotationMap.get(name);

    String[] multi = multiMapped.get(name);
    if (multi != null) {
      for (String nodeName : multi) {
        rotateOnAxis(nodeName, null, servo.getTargetPos(), velocity); // was
                                                                      // getPos()
      }
    } else {
      rotateOnAxis(name, null, servo.getTargetPos(), velocity);
    }
  }

  @Override
  public void attach(ServoControl servo, int pinOrAddress) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void onServoStop(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onServoWriteMicroseconds(ServoControl servo, int uS) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onServoSetSpeed(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onServoEnable(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onServoDisable(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void attachServoControl(ServoControl sc) {
    // TODO Auto-generated method stub
    
  }

}