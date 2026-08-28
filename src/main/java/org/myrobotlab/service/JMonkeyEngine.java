package org.myrobotlab.service;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.cv.CVData;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.jme3.AnalogHandler;
import org.myrobotlab.jme3.DepthPick;
import org.myrobotlab.jme3.HudText;
import org.myrobotlab.jme3.Interpolator;
import org.myrobotlab.jme3.Jme3App;
import org.myrobotlab.jme3.Jme3Msg;
import org.myrobotlab.jme3.Jme3Util;
import org.myrobotlab.jme3.PhysicsTestHelper;
import org.myrobotlab.jme3.Search;
import org.myrobotlab.jme3.UserData;
import org.myrobotlab.jme3.UserDataConfig;
import org.myrobotlab.kinematics.JointFrame;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.kinematics.ReachCloud;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.geometry.DepthCloudJme;
import org.myrobotlab.math.geometry.DepthColorMap;
import org.myrobotlab.math.geometry.DepthToRgbMesh;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.net.Connection;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.service.config.JMonkeyEngineConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.data.DepthFrame;
import org.myrobotlab.service.data.DepthHud;
import org.myrobotlab.service.interfaces.DepthFrameListener;
import org.myrobotlab.service.interfaces.DepthFramePublisher;
import org.myrobotlab.service.interfaces.DepthHudListener;
import org.myrobotlab.service.interfaces.DepthHudPublisher;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.PointCloudListener;
import org.myrobotlab.service.interfaces.PointCloudPublisher;
import org.myrobotlab.service.interfaces.PointListener;
import org.myrobotlab.service.interfaces.SelectListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoControlListener;
import org.myrobotlab.service.interfaces.ServoStatusListener;
import org.myrobotlab.service.interfaces.Simulator;
import org.slf4j.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.BulletAppState;
// import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.collision.CollisionResults;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
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
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

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
public class JMonkeyEngine extends Service<JMonkeyEngineConfig> implements Gateway, ActionListener, Simulator, EncoderListener, IKJointAngleListener, ServoStatusListener, ServoControlListener, PointCloudListener, DepthHudListener, DepthFrameListener {

  final static String CAMERA = "camera";

  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);

  protected final static String ROOT = "root";

  private static final long serialVersionUID = 1L;

  protected boolean altLeft = false;

  protected transient AnalogListener analog = null;

  protected transient Jme3App app;

  protected transient AssetManager assetManager;

  protected String assetsDir = getResourceDir() + File.separator + "assets";

  protected String modelsDir = assetsDir + File.separator + "Models";

  protected boolean autoAttach = true;

  protected transient Node camera = new Node(CAMERA);

  protected transient Camera cam;

  protected transient CameraNode camNode;

  protected boolean ctrlLeftPressed = false;

  protected String defaultAppType = "Jme3App";

  protected double defaultServoSpeed = 500;

  protected long deltaMs;

  protected transient DisplayMode displayMode = null;
  
  protected ChaseCamera chaseCamera;

  protected String fontColor = "#000000"; // black

  protected int fontSize = 20;

  /**
   * When true, show left/right InMoov hand world positions in the upper-left HUD.
   */
  protected boolean showHandPositions = true;

  /**
   * Robot name prefix for hand nodes (e.g. {@code i01} → {@code i01.leftHand.wrist}).
   */
  protected String handPositionRobot = "i01";

  protected static final String HAND_POSITION_HUD_KEY = "hand-positions";

  /** Live IK / hand readout on the GUI viewport (upper-left). */
  protected transient BitmapText handPositionHudText;

  protected static final String LEFT_HAND_MARKER = "_marker.leftHand";

  protected static final String RIGHT_HAND_MARKER = "_marker.rightHand";

  protected static final String IK_LEFT_HAND_MARKER = "_marker.ikLeftHand";

  /** World-space radius of the left/right hand position dots. */
  protected float handMarkerRadius = 0.05f;

  protected transient Geometry leftHandMarker;

  protected transient Geometry rightHandMarker;

  protected transient Geometry ikLeftHandMarker;

  /** IK Cartesian goal in world coordinates (green marker). */
  protected Vector3f ikLeftHandGoal;

  /** Latest IK forward-kinematics palm (HUD). */
  protected Vector3f ikLeftHandCurrent;

  /** Displayed green marker: goal if set, otherwise current FK. */
  protected Vector3f ikLeftHandWorld;

  /** Last OAK-D overlay click in world meters (Y-up), or null. */
  public Point lastClickPoint;

  /** Distance from the chest camera to {@link #lastClickPoint}, meters. */
  public float lastClickDistanceM;

  /** Last left-hand reach cloud shown in the simulator (world meters). */
  public PointCloud lastReachCloud;

  /** Voxel count of {@link #lastReachCloud}, for the WebGui. */
  public int reachCloudPointCount = 0;

  protected transient FloatBuffer reachCloudBuffer = null;

  protected transient FloatBuffer reachCloudColorBuffer = null;

  protected transient Material reachCloudMat = null;

  protected transient Mesh reachCloudMesh = null;

  protected transient Geometry reachCloudGeometry = null;

  protected int reachCloudVertexCount = 0;

  /** Chest camera world translation (copied on the render thread). */
  protected final Vector3f lastChestCameraWorld = new Vector3f();

  protected boolean fullscreen = false;

  protected transient Node guiNode;

  protected transient Map<String, HudText> guiText = new TreeMap<>();

  protected int height = 768;

  protected transient List<Jme3Msg> history = new ArrayList<Jme3Msg>();

  protected transient InputManager inputManager;

  protected transient Interpolator interpolator;

  protected transient Queue<Jme3Msg> jme3MsgQueue = new ConcurrentLinkedQueue<Jme3Msg>();
  
  /**
   * currently loaded models, if JMonkey is asked to reload a model, it will explode
   */
  final protected Set<String> loadedModels = new TreeSet<>(); 

  final public String KEY_SEPERATOR = "/";

  protected boolean mouseLeft = false;

  protected boolean mouseRightPressed = false;

  protected Map<String, String[]> multiMapped = new TreeMap<>();

  // https://stackoverflow.com/questions/16861727/jmonkey-engine-3-0-drawing-points
  protected transient FloatBuffer pointCloudBuffer = null;

  protected transient FloatBuffer pointCloudColorBuffer = null;

  protected transient Material pointCloudMat = null;

  protected transient Mesh pointCloudMesh = new Mesh();

  protected transient Geometry pointCloudGeometry = null;

  protected transient Geometry depthMeshGeometry = null;

  protected transient Mesh depthSurfaceMesh = null;

  protected transient Material depthMeshMat = null;

  protected transient Texture2D depthMeshTexture = null;

  protected transient FloatBuffer depthMeshPosBuffer = null;

  protected transient FloatBuffer depthMeshUvBuffer = null;

  protected int depthMeshVertexCount = 0;

  protected int depthMeshTexW = 0;

  protected int depthMeshTexH = 0;

  protected transient Node chestDepthCameraNode = null;

  /**
   * Depth voxels + frustum live here (child of {@link #rootNode}), not under
   * VinMoov. Parenting the cloud to {@code topStom} exploded that node's
   * bounds, z-fought with the chest/arms, and left Unshaded vertex-color state
   * on the following Lighting/PBR draws (black textures).
   */
  protected transient Node depthOverlayNode = null;

  protected transient Vector3f cachedTorsoCenterLocal = null;

  protected transient String cachedTorsoCenterParent = null;

  protected transient Geometry depthHudGeometry = null;

  protected transient Texture2D depthHudTexture = null;

  protected transient Material depthHudMat = null;

  protected int pointCloudVertexCount = 0;

  protected transient String lastChestParentName;

  protected transient Node rootNode;

  protected boolean saveHistory = false;

  protected transient Spatial selectedForMovement = null;

  protected transient Spatial selectedForView = null;

  protected int selectIndex = 0;

  protected transient AppSettings settings;

  protected boolean shiftLeft = false;

  protected long sleepMs;

  protected long startUpdateTs;

  protected transient AppStateManager stateManager;

  protected transient Jme3Util util;

  protected transient ViewPort viewPort;

  protected int width = 1024;
  
  protected float orbitRadius = 10f;
  
  protected float orbitSpeed = 1.0f;

  protected float orbitMinDistance = 0.3f;

  protected float orbitMaxDistance = 80f;

  protected float panSpeed = 1.0f;

  /** Extra translation applied to the orbit look-at point after panning. */
  protected final Vector3f orbitPanOffset = new Vector3f();

  /** True after the mouse has moved while a button is held (orbit/pan vs click-select). */
  protected boolean viewDragging = false;

  protected float viewDragAccum = 0f;
  
  protected float mouseX = 0f;
  
  protected float mouseY = 0f;

  /** Radians of orbit per pixel dragged. */
  protected float orbitRadiansPerPixel = 0.008f;

  // protected Set<String> modelPaths = new LinkedHashSet<>();

  protected Map<String, UserData> nodes = new LinkedHashMap<>();

  /**
   * current selected path
   */
  protected String selectedPath = null;

  protected boolean mouseMiddle = false;

  public JMonkeyEngine(String n, String id) {
    super(n, id);
    util = new Jme3Util(this);
    analog = new AnalogHandler(this);
    interpolator = new Interpolator(this, util);

    // setup the virtual reflection
    // this will "connect" to our mrl instance
    // and part of the connection is the mrl instance
    // sending a series of registrations ... including self
    // still a race condition ?
    try {
      connect("jme://local/messages");
    } catch (Exception ignored) {
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

    if (check instanceof Geometry) {
      log.error("addBox - scene graph already has {} and it is a Geometry", check);
      return null;
    } else if (check instanceof Node) {
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
      log.warn("addGrid {} already exists", name);
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

  public void addNode(String name) {
    addMsg("addNode", name);
  }

  // Routing Attach - should be based on string type info and name (ie a
  // Registration)
  @Override
  public void attach(Attachable attachable) throws Exception {
    String name = attachable.getName();
    ServiceInterface service = Runtime.getService(name);
    if (service == null) {
      log.error("{} not found in registry", name);
      return;
    }
    
    if (service instanceof SelectListener) {
      addListener("getSelectedPath", service.getName(), "onSelected");
    }

    // FIXME 2023-06-21 GroG: interested services SHOULD NOT evaluate by type, they
    // should evaluate how to attach by INTERFACE - the following should be refactored
    // to subscribe based on interface not type
    
    // We do type evaluation and routing based on string values vs instance
    // values
    // this is to support future (non-Java) classes that cannot be instantiated
    // and
    // are subclassed in a proxy class with getType() overloaded for to identify
    /**<pre> DO NOT NEED THIS UNTIL JMONKEY DISPLAYS VIDEO DATA - SLAM MAPPING
    if (service.getTypeKey().equals("org.myrobotlab.service.OpenCV")) {
      AbstractComputerVision cv = (AbstractComputerVision) service;
      subscribe(service.getName(), "publishCvData");
    }</pre>
     */

    if (service instanceof PointCloudPublisher) {
      subscribe(service.getName(), "publishPointCloud", getName(), "onPointCloud");
      wireDepthClickToIk();
    }
    if (service instanceof DepthHudPublisher) {
      subscribe(service.getName(), "publishDepthHud", getName(), "onDepthHud");
    }
    if (service instanceof DepthFramePublisher) {
      subscribe(service.getName(), "publishDepthFrame", getName(), "onDepthFrame");
      subscribe(service.getName(), "publishRgbMesh", getName(), "onRgbMesh");
    }
    if (service instanceof OakD) {
      addListener("publishClickPoint", service.getName(), "onSimulatorClick");
    }
    if (service instanceof PointListener) {
      addListener("publishClickPoint", service.getName(), "onPoint");
    }

    if (service.getTypeKey().equals("org.myrobotlab.service.Servo")) {
      // Instantaneous angle stream (TimeEncoder) and direct move commands
      subscribe(service.getName(), "publishEncoderData", getName(), "onEncoderData");
      // Servo.processMove publishes ServoMove (not ServoControl) — handle both
      subscribe(service.getName(), "publishServoMoveTo", getName(), "onServoMove");
      subscribe(service.getName(), "publishMoveTo", getName(), "onServoMoveTo");
    }

    if (service instanceof InverseKinematics3D || service instanceof Fabrik) {
      subscribe(service.getName(), "publishWorldPosition", getName(), "onWorldPosition");
      subscribe(service.getName(), "publishIkGoal", getName(), "onIkGoal");
    }
    if (service instanceof Fabrik) {
      subscribe(service.getName(), "publishReachCloud", getName(), "onReachCloud");
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
   *          child
   * @param parent
   *          parent
   * 
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
   * need to change, but has a higher likely hood of collisions.
   * 
   * @param tree
   *          t
   * @param path
   *          p
   * @param spatial
   *          s
   * @param includeGeometries
   *          include
   * @param useDepthKeys
   *          depth
   * @return map of user data
   * 
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
      for (Spatial child : children) {
        buildTree(tree, path, child, includeGeometries, useDepthKeys);
      }
    }
    return tree;
  }
  
  public void resetView() {
    orbitPanOffset.set(0, 0, 0);
    camera.setLocalTransform(new Transform(new Vector3f(0, 3, 5)));
    cameraLookAt("root");
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
    orbitPanOffset.set(0, 0, 0);
    addMsg("lookAt", CAMERA, spatial.getName());
  }

  public void cameraLookAt(String name) {
    JMonkeyEngineConfig c = (JMonkeyEngineConfig) config;
    Spatial s = get(name);
    if (s == null) {
      log.error("cameraLookAt - cannot find {}", name);
      return;
    }
    c.cameraLookAt = name;
    cameraLookAt(s);
  }

  public void cameraLookAtRoot() {
    cameraLookAt(rootNode);
  }

  /**
   * Ray-pick from the cursor. A click on the visible OAK-D voxel cloud or RGB
   * mesh publishes {@link #publishClickPoint} (world meters, Y-up) for Fabrik
   * and does not steal the orbit selection.
   */
  public Geometry checkCollision() {
    if (cam == null || inputManager == null || rootNode == null) {
      return null;
    }
    refreshDepthCollisionData();
    CollisionResults results = new CollisionResults();
    Vector2f click2d = inputManager.getCursorPosition();
    Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
    Vector3f dir = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
    Ray ray = new Ray(click3d, dir);
    rootNode.collideWith(ray, results);
    DepthPick.Hit hit = DepthPick.firstPick(results);
    if (hit == null) {
      return null;
    }
    log.info("you clicked {} at {}", hit.geometry.getName(), hit.world);
    JMonkeyEngineConfig cfg = config;
    if (hit.depthOverlay && (cfg == null || cfg.depthClickToIk)) {
      Point p = new Point(hit.world.x, hit.world.y, hit.world.z);
      onIkGoal(p);
      invoke("publishClickPoint", p);
    }
    return hit.geometry;
  }

  /**
   * Rebuild triangle collision trees for the visible OAK-D overlay. Vertex
   * buffers change every frame; stale BIH data would pick the wrong point.
   */
  private void refreshDepthCollisionData() {
    refreshMeshCollision(pointCloudGeometry, pointCloudMesh);
    refreshMeshCollision(depthMeshGeometry, depthSurfaceMesh);
  }

  private void refreshMeshCollision(Geometry geometry, Mesh mesh) {
    if (geometry == null || mesh == null || DepthPick.isCulled(geometry)) {
      return;
    }
    mesh.updateBound();
    mesh.updateCounts();
    try {
      mesh.createCollisionData();
      geometry.updateModelBound();
    } catch (Exception e) {
      log.warn("Could not rebuild OAK-D overlay collision data for {}", geometry.getName(), e);
    }
  }

  /**
   * World-meter point on the OAK-D overlay (same frame as Fabrik / IK).
   */
  public Point publishClickPoint(Point point) {
    lastClickPoint = point;
    lastClickDistanceM = distanceToChestCamera(point);
    log.info("OAK-D click {}  {} m from camera  depthScale {}", point, lastClickDistanceM, config.depthCloudScale);
    invoke("publishClickDistance", lastClickDistanceM);
    return point;
  }

  /** Meters from the chest camera to the last overlay click. */
  public float publishClickDistance(float meters) {
    return meters;
  }

  /**
   * Camera-frame meters → overlay local meters. Default 1 is real-world
   * (JME / IK meters). {@link JMonkeyEngineConfig#depthCloudScale} is a
   * calibration multiplier.
   */
  public float depthVertexScale() {
    JMonkeyEngineConfig cfg = config;
    float parentScale = 1f;
    if (cfg != null && cfg.depthCloudMatchWorldMeters) {
      parentScale = overlayWorldScale();
    }
    return DepthCloudJme.effectiveScale(cfg != null ? cfg.depthCloudScale : 1f, parentScale);
  }

  private float overlayWorldScale() {
    if (depthOverlayNode == null) {
      return 1f;
    }
    Vector3f ws = depthOverlayNode.getWorldScale();
    return (Math.abs(ws.x) + Math.abs(ws.y) + Math.abs(ws.z)) / 3f;
  }

  public float getDepthCloudScale() {
    return config != null && config.depthCloudScale > 0f ? config.depthCloudScale : 1f;
  }

  /**
   * Calibration multiplier on camera-frame meters. {@code 1} is real-world.
   */
  public float setDepthCloudScale(float scale) {
    if (config == null) {
      return Float.NaN;
    }
    float s = scale;
    if (s < 0.05f) {
      s = 0.05f;
    }
    if (s > 20f) {
      s = 20f;
    }
    config.depthCloudScale = s;
    log.info("depth overlay scale {}", s);
    broadcastState();
    return s;
  }

  /**
   * Set {@link JMonkeyEngineConfig#depthCloudScale} so the last mesh click
   * lands at {@code knownDistanceM} from the chest camera (tape measure).
   */
  public float calibrateDepthScale(double knownDistanceM) {
    if (lastClickPoint == null || lastClickDistanceM < 1e-4f) {
      error("Click the OAK-D mesh first, then calibrate with a measured distance");
      return getDepthCloudScale();
    }
    float next = DepthCloudJme.nextScale(getDepthCloudScale(), lastClickDistanceM, knownDistanceM);
    info("Depth scale %.3f → %.3f (click was %.3f m, measured %.3f m)", getDepthCloudScale(), next, lastClickDistanceM,
        knownDistanceM);
    float applied = setDepthCloudScale(next);
    lastClickDistanceM = (float) knownDistanceM;
    return applied;
  }

  private float distanceToChestCamera(Point point) {
    if (point == null) {
      return 0f;
    }
    float dx = (float) point.getX() - lastChestCameraWorld.x;
    float dy = (float) point.getY() - lastChestCameraWorld.y;
    float dz = (float) point.getZ() - lastChestCameraWorld.z;
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Subscribe running Fabrik services to mesh clicks (idempotent). Called when
   * an OAK-D (or other point-cloud publisher) is attached.
   */
  protected void wireDepthClickToIk() {
    for (ServiceInterface si : Runtime.getServices()) {
      if (si instanceof PointListener) {
        addListener("publishClickPoint", si.getName(), "onPoint");
      }
    }
  }

  public void clone(String name, String newName) {

  }

  @Override
  public void connect(String uri) throws Exception {

    String uuid = java.util.UUID.randomUUID().toString();
    String id = getName() + "-" + Runtime.getInstance().getId() + "-jme";
    Connection attributes = new Connection(uuid, id, getName());

    attributes.put("c-type", getSimpleName());
    Runtime.getInstance().addConnection(uuid, id, attributes);
    // Runtime.getInstance().updateRoute(guiId, uuid);
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

    if (shiftLeft) {
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

    /*
     * mmm - may be a bad idea - but may need to figure solution out.. if
     * (spatial instanceof Geometry) { UserData data =
     * jme.getUserData(spatial.getParent()); data.enableCoordinateAxes(b);
     * return; }
     */

    if (spatial.getName().startsWith("_")) {
      log.warn("enableAxes({}) a meta object not creating/enabling", spatial.getName());
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
      log.warn("enableBoundingBox({}) begins with \"_\" is a meta node - will not create new bounding box", name);
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

  // FIXME  -  use ctrl space like blender ...
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
   * @return spatial object
   */
  public Spatial find(String name, Node startNode) {
    if (name.equals(ROOT)) {
      return rootNode;
    }
    if (startNode == null) {
      startNode = rootNode;
    }

    return startNode.getChild(name);
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
   *          name
   * @param startNode
   *          starting node
   * @return spatial object.
   * 
   */
  public Spatial get(String name, Node startNode) {
    Spatial ret = find(name, startNode);
    if (ret == null) {
      log.info("get({}) could not find child", name);
    }
    return ret;
  }

  /**
   * get default axis local rotation in degrees
   * 
   * @param name
   *          name of joint
   * @return angle in degrees
   * 
   */
  public Float getAngle(String name) {
    return getAngle(name, null);
  }

  /**
   * The joint's input (servo) angle. Read from the angle last applied by
   * {@link Jme3Util#rotateTo}, which is exact — recovering it from the node's
   * quaternion via Euler angles is lossy and clamps the Z axis to ±90°.
   */
  public Float getAngle(String name, String axis) {
    Spatial s = get(name);
    if (s == null) {
      return null;
    }
    UserData data = getUserData(name);
    if (data == null) {
      return null;
    }
    double meshAngle = data.getCurrentAngleDeg();
    if (data.mapper != null) {
      return Double.valueOf(data.mapper.calcInput(meshAngle)).floatValue();
    }
    return (float) meshAngle;
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
    return String.format("_bb-%s-%s", getType(spatial), spatial.getName());
  }

  @Override
  public List<String> getClientIds() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Connection> getClients() {
    return Runtime.getInstance().getConnections(getName());
  }

  public String getCoorAxesName(Spatial spatial) {
    if (spatial.getName().startsWith("_")) {
      return null;
    }
    return String.format("_axis-%s-%s", getType(spatial), spatial.getName());
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

  public Node getMenuNode() {
    return guiNode;
  }

  public Map<String, String[]> getMultiMapped() {
    return multiMapped;
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
  
  /**
   * Set selected path updates the current selectedPath to from a ray
   * collision in the scene graph. The collision is currently implemented
   * as a mouse click.  The point at where the mouse is clicked a "path" to
   * an object collision is created and set an published through getSelectedPath.
   * This publication can be picked up by other services if they need such 
   * events.
   *  
   * @param path
   * @return
   */
  public String setSelectedPath(String path) {
    selectedPath = path;
    if (path != null) {
      invoke("getSelectedPath");
    }
    return path;
  }

  /**
   * selected path is the ORIGINAL_PATH of the selected node 
   * @return
   */
  public String getSelectedPath() {
    return selectedPath;
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
      // not sure if this is right - using the nodeName as "path"
      data = new UserData(this, node);
      String nodeName = node.getName();
      if (nodes.containsKey(nodeName)) {
        error("collision on node name %s", nodeName);
      }
      nodes.put(nodeName, data);
      // FIXME - add map/index
      // getAncestorKey(x) + rootKey if its not root = key
    }
    return data;
  }

  /**
   * The workhorse - where everyone "searches" for the user data they need. It
   * works against a flat or depth key'd tree. If the node is found but the user
   * data has not been created, it creates it and assigns the references... if
   * the node cannot be found, it returns null
   * 
   * @param path
   *          - full path for a depth tree, name for a flat map
   * @return userdata
   */
  public UserData getUserData(String path /* , boolean useDepth */) {

    Spatial spatial = get(path);

    if (spatial == null) {
      log.warn("geteUserData {} cannot be found", path);
      return null;
    }

    if (spatial instanceof Geometry) {
      log.warn("geteUserData {} found but is Geometry not Node", path);
      return null;
    }

    UserData userData = spatial.getUserData("data");
    if (userData == null) {
      userData = new UserData(this, spatial);

      if (this.nodes.containsKey(path)) {
        error("collision on node name %s", path);
      }
      this.nodes.put(path, userData);
    }
    return userData;
  }

  public void hide(String name) {
    setVisible(name, false);
  }

  public void initPointCloud(PointCloud pc) {
    Point3df[] points = pc.getData();
    int n = points == null ? 0 : points.length;
    pointCloudVertexCount = n;
    int verts = Math.max(1, n) * 8;
    pointCloudBuffer = BufferUtils.createFloatBuffer(verts * 3);
    pointCloudColorBuffer = BufferUtils.createFloatBuffer(verts * 4);
    writePointCloudBuffers(pc);

    pointCloudMesh = new Mesh();
    pointCloudMesh.setMode(Mesh.Mode.Triangles);
    pointCloudMesh.setBuffer(VertexBuffer.Type.Position, 3, pointCloudBuffer);
    pointCloudMesh.setBuffer(VertexBuffer.Type.Color, 4, pointCloudColorBuffer);
    pointCloudMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(DepthCloudJme.cubeIndices(Math.max(1, n))));
    pointCloudMesh.updateBound();
    pointCloudMesh.updateCounts();

    if (pointCloudGeometry != null) {
      pointCloudGeometry.removeFromParent();
    }
    pointCloudGeometry = new Geometry(DepthPick.DEPTH_CLOUD, pointCloudMesh);
    DepthPick.mark(pointCloudGeometry);
    pointCloudMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    pointCloudMat.setBoolean("VertexColor", true);
    pointCloudMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Off);
    pointCloudMat.getAdditionalRenderState().setPolyOffset(1f, 1f);
    pointCloudGeometry.setMaterial(pointCloudMat);
    pointCloudGeometry.setShadowMode(ShadowMode.Off);
    pointCloudGeometry.setQueueBucket(Bucket.Opaque);

    ensureDepthOverlayNode();
    depthOverlayNode.attachChild(pointCloudGeometry);
    syncDepthOverlayPose();
    applyDepthDisplayMode();
  }

  /**
   * Camera frame (X right, Y down, Z forward) → JME local (X right, Y up, Z
   * forward). Each sample is a cube on {@link #depthOverlayNode} (not under
   * VinMoov) so torso motion is copied via {@link #syncDepthOverlayPose()}.
   */
  private void writePointCloudBuffers(PointCloud pc) {
    Point3df[] points = pc.getData();
    float[] colors = pc.getColors();
    int n = points == null ? 0 : points.length;
    JMonkeyEngineConfig cfg = config;
    float scale = depthVertexScale();
    float voxel = cfg != null ? cfg.depthCloudVoxelM : 0.03f;
    float half = Math.max(0.004f, voxel * 0.5f);
    float parentScale = overlayWorldScale();
    if (cfg != null && cfg.depthCloudMatchWorldMeters && parentScale > 1e-6f) {
      half = half / parentScale;
    }

    pointCloudBuffer.clear();
    pointCloudColorBuffer.clear();
    float[] corners = new float[24];
    for (int i = 0; i < n; i++) {
      Point3df p = points[i];
      DepthCloudJme.voxelCorners(p.x, p.y, p.z, scale, half, corners);
      for (int c = 0; c < 24; c++) {
        pointCloudBuffer.put(corners[c]);
      }
      float r = 0.2f;
      float g = 0.9f;
      float b = 0.3f;
      float a = 1f;
      if (colors != null && colors.length >= (i + 1) * 4) {
        r = colors[i * 4];
        g = colors[i * 4 + 1];
        b = colors[i * 4 + 2];
        a = colors[i * 4 + 3];
      }
      for (int c = 0; c < 8; c++) {
        pointCloudColorBuffer.put(r).put(g).put(b).put(a);
      }
    }
    if (n == 0) {
      for (int i = 0; i < 24; i++) {
        pointCloudBuffer.put(0f);
      }
      for (int i = 0; i < 8; i++) {
        pointCloudColorBuffer.put(0f).put(0f).put(0f).put(0f);
      }
    }
    pointCloudBuffer.flip();
    pointCloudColorBuffer.flip();
  }

  /**
   * Organized depth grid → RGB-textured triangles on {@link #depthOverlayNode}.
   * Camera Y is flipped to JME Y-up, same as voxels.
   */
  protected void initDepthRgbMesh(DepthToRgbMesh.Result mesh, byte[] rgb, int texW, int texH) {
    int n = Math.max(1, mesh.vertexCount);
    depthMeshVertexCount = mesh.vertexCount;
    depthMeshPosBuffer = BufferUtils.createFloatBuffer(n * 3);
    depthMeshUvBuffer = BufferUtils.createFloatBuffer(n * 2);
    writeDepthMeshBuffers(mesh, rgb, texW, texH);

    depthSurfaceMesh = new Mesh();
    depthSurfaceMesh.setMode(Mesh.Mode.Triangles);
    depthSurfaceMesh.setBuffer(VertexBuffer.Type.Position, 3, depthMeshPosBuffer);
    depthSurfaceMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, depthMeshUvBuffer);
    int[] idx = mesh.indices != null && mesh.indices.length > 0 ? mesh.indices : new int[] { 0, 0, 0 };
    depthSurfaceMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(idx));
    depthSurfaceMesh.updateBound();
    depthSurfaceMesh.updateCounts();

    if (depthMeshGeometry != null) {
      depthMeshGeometry.removeFromParent();
    }
    depthMeshGeometry = new Geometry(DepthPick.DEPTH_RGB_MESH, depthSurfaceMesh);
    DepthPick.mark(depthMeshGeometry);
    depthMeshMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    depthMeshMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Off);
    depthMeshMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
    depthMeshMat.getAdditionalRenderState().setPolyOffset(1f, 1f);
    uploadDepthMeshTexture(rgb, texW, texH);
    depthMeshMat.setTexture("ColorMap", depthMeshTexture);
    depthMeshGeometry.setMaterial(depthMeshMat);
    depthMeshGeometry.setShadowMode(ShadowMode.Off);
    depthMeshGeometry.setQueueBucket(Bucket.Opaque);

    ensureDepthOverlayNode();
    depthOverlayNode.attachChild(depthMeshGeometry);
    syncDepthOverlayPose();
    applyDepthDisplayMode();
  }

  private void writeDepthMeshBuffers(DepthToRgbMesh.Result mesh, byte[] rgb, int texW, int texH) {
    float scale = depthVertexScale();

    depthMeshPosBuffer.clear();
    int n = mesh.vertexCount;
    float[] p = mesh.positions;
    for (int i = 0; i < n; i++) {
      int o = i * 3;
      depthMeshPosBuffer.put(p[o] * scale);
      depthMeshPosBuffer.put(-p[o + 1] * scale);
      depthMeshPosBuffer.put(p[o + 2] * scale);
    }
    if (n == 0) {
      depthMeshPosBuffer.put(0f).put(0f).put(0f);
    }
    depthMeshPosBuffer.flip();

    depthMeshUvBuffer.clear();
    if (mesh.uvs != null && mesh.uvs.length >= n * 2) {
      depthMeshUvBuffer.put(mesh.uvs, 0, n * 2);
    }
    if (n == 0) {
      depthMeshUvBuffer.put(0f).put(0f);
    }
    depthMeshUvBuffer.flip();

    if (depthSurfaceMesh != null) {
      int[] idx = mesh.indices != null && mesh.indices.length > 0 ? mesh.indices : new int[] { 0, 0, 0 };
      depthSurfaceMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(idx));
    }
    uploadDepthMeshTexture(rgb, texW, texH);
  }

  private void uploadDepthMeshTexture(byte[] rgb, int w, int h) {
    if (rgb == null || w <= 0 || h <= 0 || rgb.length < w * h * 3) {
      return;
    }
    ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 3);
    for (int y = 0; y < h; y++) {
      int src = (h - 1 - y) * w * 3;
      buf.put(rgb, src, w * 3);
    }
    buf.flip();
    Image image = new Image(Image.Format.RGB8, w, h, buf, ColorSpace.sRGB);
    if (depthMeshTexture == null || depthMeshTexW != w || depthMeshTexH != h) {
      depthMeshTexture = new Texture2D(image);
      depthMeshTexture.setMagFilter(Texture.MagFilter.Bilinear);
      depthMeshTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
      depthMeshTexture.setWrap(Texture.WrapMode.EdgeClamp);
      depthMeshTexW = w;
      depthMeshTexH = h;
      if (depthMeshMat != null) {
        depthMeshMat.setTexture("ColorMap", depthMeshTexture);
      }
    } else {
      depthMeshTexture.setImage(image);
    }
  }

  private static byte[] meshTextureRgb(DepthFrame frame, int[] wh) {
    if (frame.rgb != null && frame.rgbWidth > 0 && frame.rgbHeight > 0
        && frame.rgb.length >= frame.rgbWidth * frame.rgbHeight * 3) {
      wh[0] = frame.rgbWidth;
      wh[1] = frame.rgbHeight;
      return frame.rgb;
    }
    DepthHud hud = DepthColorMap.toHud(frame);
    if (hud == null || hud.rgb == null) {
      return null;
    }
    wh[0] = hud.width;
    wh[1] = hud.height;
    return hud.rgb;
  }

  protected void updateRgbMeshOnRenderThread(DepthFrame frame) {
    ensureChestDepthCameraOnRenderThread(config);
    syncDepthOverlayPose();
    JMonkeyEngineConfig cfg = config;
    if (cfg != null && !cfg.depthCloud) {
      applyDepthDisplayMode();
      return;
    }
    float maxEdge = cfg != null ? cfg.depthMeshMaxEdgeM : DepthToRgbMesh.DEFAULT_MAX_EDGE_M;
    DepthToRgbMesh.Result mesh = DepthToRgbMesh.convert(frame, maxEdge);
    int[] wh = new int[2];
    byte[] rgb = meshTextureRgb(frame, wh);
    if (rgb == null || mesh.vertexCount <= 0) {
      applyDepthDisplayMode();
      return;
    }
    if (depthMeshGeometry == null || depthMeshPosBuffer == null || mesh.vertexCount != depthMeshVertexCount) {
      initDepthRgbMesh(mesh, rgb, wh[0], wh[1]);
      return;
    }
    writeDepthMeshBuffers(mesh, rgb, wh[0], wh[1]);
    depthSurfaceMesh.setBuffer(VertexBuffer.Type.Position, 3, depthMeshPosBuffer);
    depthSurfaceMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, depthMeshUvBuffer);
    depthSurfaceMesh.updateBound();
    depthSurfaceMesh.updateCounts();
    applyDepthDisplayMode();
  }

  public void setDepthRgbMesh(boolean enabled) {
    config.depthRgbMesh = enabled;
    if (app != null) {
      app.enqueue(() -> {
        applyDepthDisplayMode();
        return null;
      });
    }
  }

  public void onRgbMesh(Boolean enabled) {
    setDepthRgbMesh(Boolean.TRUE.equals(enabled));
  }

  private void applyDepthDisplayMode() {
    JMonkeyEngineConfig cfg = config;
    boolean show3d = cfg == null || cfg.depthCloud;
    boolean mesh = cfg != null && cfg.depthRgbMesh;
    if (pointCloudGeometry != null) {
      pointCloudGeometry.setCullHint((show3d && !mesh) ? CullHint.Inherit : CullHint.Always);
    }
    if (depthMeshGeometry != null) {
      depthMeshGeometry.setCullHint((show3d && mesh) ? CullHint.Inherit : CullHint.Always);
    }
  }

  @Override
  public boolean isLocal(Message msg) {
    return false;
  }

  // FIXME - more parameters - location & rotation (new function "move")
  // FIXME - scale should not be in this - scale as one of 3 methods rotate !!!!
  // translate
  // TODO - must be re-entrant - perhaps even on a schedule ?
  // TODO - removeNode
  public void loadResource(String inFileName) {
    log.info("loadResource({})", inFileName);

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
        // hmmm - absolute paths ? this is fragile
        // FIXME - somehow make relative work
        Node node = null;

        if (spatial instanceof Node) {
          node = (Node) spatial;
        } else {
          node = new Node(spatial.getName());
          node.attachChild(spatial);
        }

        rootNode.attachChild(node);

      } else {

        // FIXME - put msgs into yml form
        String json = FileIO.toString(filename);
        Jme3Msg[] msgs = CodecUtils.fromJson(json, Jme3Msg[].class);
        log.info("adding {} msgs", msgs.length);
        Collections.addAll(jme3MsgQueue, msgs);
      }

    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * load a known file type
   * 
   * @param inFileName
   *          input file name
   */
  public void loadFile(String inFileName) {
    File file = getFile(inFileName);
    if (!file.exists()) {
      error("%s does not exist", inFileName);
    }
  }

  /**
   * Load a specific model file
   * @param assetPath
   * @return
   */
  public Spatial loadModel(String assetPath) {
    JMonkeyEngineConfig c = (JMonkeyEngineConfig) config;
    Spatial model = null;
    try {
      String basename = modelBasename(assetPath);
      if (loadedModels.contains(assetPath) || loadedModels.contains(basename)
          || isModelBasenameLoaded(basename)) {
        log.info("model {} already loaded (skipping duplicate)", assetPath);
        return null;
      }

      // Already have a VinMoov / robot root in the scene — do not attach another body
      if (isVinMoovAsset(basename) && hasVinMoovOrRobotRoot()) {
        log.info("VinMoov/robot root already in scene — skipping {}", assetPath);
        loadedModels.add(basename);
        loadedModels.add(assetPath);
        return null;
      }
      
      if (FileIO.checkDir(modelsDir + fs + assetPath)) {
        log.info("skipping directory {}", assetPath);
        return null;        
      }
      
      if (assetPath.toLowerCase().endsWith(".md") || assetPath.toLowerCase().endsWith(".txt") || assetPath.toLowerCase().endsWith(".bin")) {
        log.info("skipping {} not a valid model type", assetPath);
        return null;
      }
      
      log.info("loading {}", assetPath);
      model = assetManager.loadModel(assetPath);
      log.info("loaded {} name={}", assetPath, model != null ? model.getName() : null);
      if (model != null) {
        if (model.getName() == null || model.getName().isEmpty() || model.getName().equals(assetPath)) {
          model.setName(basename);
        }
        getRootNode().attachChild(model);
        loadedModels.add(assetPath);
        loadedModels.add(basename);
      } else {
        error("%s model null", assetPath);
      }
      
      if (c.models == null) {
        c.models = new ArrayList<>();
      }
      
      if (!c.models.contains(assetPath) && !c.models.contains(basename)) {
        c.models.add(basename.endsWith(".j3o") || basename.contains(".") ? assetPath : basename + ".j3o");
      }
    } catch(Exception e) {
      error(e);
    }
    return model;
  }

  private static String modelBasename(String assetPath) {
    String simple = assetPath.replace('\\', '/');
    int slash = simple.lastIndexOf('/');
    if (slash >= 0) {
      simple = simple.substring(slash + 1);
    }
    return simple;
  }

  private static String modelNameNoExt(String assetPath) {
    String simple = modelBasename(assetPath);
    int dot = simple.lastIndexOf('.');
    if (dot > 0) {
      return simple.substring(0, dot);
    }
    return simple;
  }

  private static boolean isVinMoovAsset(String basename) {
    String n = modelNameNoExt(basename).toLowerCase();
    return n.startsWith("vinmoov");
  }

  private boolean isModelBasenameLoaded(String basename) {
    String noExt = modelNameNoExt(basename);
    for (String loaded : loadedModels) {
      if (modelBasename(loaded).equalsIgnoreCase(basename) || modelNameNoExt(loaded).equalsIgnoreCase(noExt)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasVinMoovOrRobotRoot() {
    if (rootNode == null) {
      return false;
    }
    for (Spatial child : rootNode.getChildren()) {
      String n = child.getName();
      if (n == null) {
        continue;
      }
      if (n.equals("i01") || n.toLowerCase().startsWith("vinmoov")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Remove extra VinMoov clones so only one body remains (named robotName when possible).
   */
  public void removeDuplicateVinMoovRoots(String robotName) {
    if (rootNode == null) {
      return;
    }
    List<Spatial> bodies = new ArrayList<>();
    for (Spatial child : new ArrayList<>(rootNode.getChildren())) {
      String n = child.getName();
      if (n == null) {
        continue;
      }
      if (n.equals(robotName) || n.toLowerCase().startsWith("vinmoov")) {
        bodies.add(child);
      }
    }
    if (bodies.size() <= 1) {
      if (bodies.size() == 1 && robotName != null && !robotName.equals(bodies.get(0).getName())) {
        String old = bodies.get(0).getName();
        bodies.get(0).setName(robotName);
        log.info("Renamed sole body {} -> {}", old, robotName);
      }
      return;
    }
    // Keep the first; detach the rest
    Spatial keep = bodies.get(0);
    keep.setName(robotName != null ? robotName : keep.getName());
    for (int i = 1; i < bodies.size(); i++) {
      Spatial dup = bodies.get(i);
      log.warn("Removing duplicate VinMoov body {}", dup.getName());
      dup.removeFromParent();
    }
  }


  /**
   * load a node with all potential children
   * 
   * @param parentDirPath
   *          p
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
   *          dir
   *
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
    log.debug("onAction {} {} {}", name, keyPressed, tpf);

    if ("full-screen".equals(name)) {
      enableFullScreen(true);
    } else if ("select-root".equals(name)) {
      setSelected(rootNode);
    } else if (CAMERA.equals(name)) {
      setSelected(CAMERA);
    } else if ("exit-full-screen".equals(name)) {
      enableFullScreen(false);
    } else if ("cycle".equals(name) && keyPressed) {
      cycle();
    } else if (name.equals("shift-left")) {
      shiftLeft = keyPressed;
    } else if (name.equals("ctrl-left")) {
      ctrlLeftPressed = keyPressed;
    } else if (name.equals("alt-left")) {
      altLeft = keyPressed;
    } else if ("export".equals(name) && keyPressed) {
      saveSpatial(selectedForView.getName());
    } else if ("mouse-click-left".equals(name)) {
      mouseLeft = keyPressed;
      // Click (no drag) selects; drag orbits the view. Selecting on press would
      // jump the orbit target mid-gesture.
      if (mouseLeft) {
        viewDragging = false;
        viewDragAccum = 0f;
        captureCursor();
      } else {
        if (!viewDragging) {
          Geometry target = checkCollision();
          // Mesh clicks publish an IK goal — keep the current orbit target.
          if (target == null || !DepthPick.isDepthOverlay(target)) {
            setSelected(target);
          }
        }
        viewDragging = false;
        viewDragAccum = 0f;
      }
    } else if ("mouse-click-middle".equals(name)) {
      mouseMiddle = keyPressed;
      if (mouseMiddle) {
        captureCursor();
      } else {
        viewDragging = false;
      }
    } else if ("mouse-click-right".equals(name)) {
      mouseRightPressed = keyPressed;
      if (mouseRightPressed) {
        captureCursor();
      } else {
        viewDragging = false;
      }
    } else {
      warn("%s - key %b %f not found", name, keyPressed, tpf);
    }
  }

  /**
   * onAnalog
   * 
   * @param name
   *          name
   * @param keyPressed
   *          key pressed
   * @param tpf
   *          tfp
   *
   */
  public void onAnalog(String name, float keyPressed, float tpf) {
    log.debug("onAnalog [{} {} {}]", name, keyPressed, tpf);

    // Mouse X/Y analog does not fire with a visible cursor under LWJGL3.
    // Orbit/pan is applied from cursor deltas in simpleUpdate instead.
    if (name.equals("mouse-wheel-up") || name.equals("mouse-wheel-down")) {
      zoomCamera(name.equals("mouse-wheel-up") ? -1f : 1f);
    }
  }

  private void captureCursor() {
    if (inputManager == null) {
      return;
    }
    Vector2f cursor = inputManager.getCursorPosition();
    mouseX = cursor.x;
    mouseY = cursor.y;
  }

  /**
   * Orbit / pan from absolute cursor movement. Required because
   * {@link MouseAxisTrigger} X/Y values are not delivered when the cursor is
   * visible (LWJGL3 ungrabbed mouse). Wheel analog still works.
   */
  protected void updateViewDrag() {
    if (inputManager == null || camera == null) {
      return;
    }
    Vector2f cursor = inputManager.getCursorPosition();
    boolean buttonDown = mouseLeft || mouseMiddle || mouseRightPressed;
    if (!buttonDown) {
      mouseX = cursor.x;
      mouseY = cursor.y;
      return;
    }
    float dx = cursor.x - mouseX;
    float dy = cursor.y - mouseY;
    mouseX = cursor.x;
    mouseY = cursor.y;
    if (dx == 0f && dy == 0f) {
      return;
    }
    applyViewDrag(dx, dy);
  }

  /**
   * Apply a pixel mouse delta: left/middle drag orbits, right or shift-drag pans.
   */
  protected void applyViewDrag(float dxPixels, float dyPixels) {
    viewDragAccum += FastMath.abs(dxPixels) + FastMath.abs(dyPixels);
    if (viewDragAccum > 3f) {
      viewDragging = true;
    }
    boolean pan = mouseRightPressed || ((mouseLeft || mouseMiddle) && shiftLeft);
    if (pan) {
      panCameraPixels(dxPixels, dyPixels);
    } else if (mouseLeft || mouseMiddle) {
      float sens = orbitRadiansPerPixel * orbitSpeed;
      orbitCamera(-dxPixels * sens, dyPixels * sens);
    }
  }

  /**
   * World-space point the camera orbits around: the configured look-at node, or
   * the scene origin, plus any pan offset.
   */
  protected Vector3f getOrbitTarget() {
    Spatial target = null;
    if (config instanceof JMonkeyEngineConfig) {
      String lookAt = ((JMonkeyEngineConfig) config).cameraLookAt;
      if (lookAt != null) {
        target = get(lookAt);
      }
    }
    if (target == null) {
      target = rootNode;
    }
    Vector3f base = target != null ? target.getWorldTranslation() : Vector3f.ZERO;
    return base.add(orbitPanOffset);
  }

  /**
   * Orbit the camera around {@link #getOrbitTarget()} by yaw (world Y) and pitch
   * (camera right). Keeps looking at the target.
   */
  protected void orbitCamera(float yaw, float pitch) {
    if (camera == null || (yaw == 0f && pitch == 0f)) {
      return;
    }
    Vector3f target = getOrbitTarget();
    Vector3f offset = camera.getWorldTranslation().subtract(target);
    if (offset.lengthSquared() < 1e-8f) {
      offset = new Vector3f(0f, 0f, orbitRadius);
    }

    Quaternion yawQ = new Quaternion();
    yawQ.fromAngleAxis(-yaw, Vector3f.UNIT_Y);
    offset = yawQ.mult(offset);

    if (pitch != 0f) {
      Vector3f right = Vector3f.UNIT_Y.cross(offset);
      if (right.lengthSquared() < 1e-8f) {
        right = Vector3f.UNIT_X.clone();
      } else {
        right.normalizeLocal();
      }
      Quaternion pitchQ = new Quaternion();
      pitchQ.fromAngleAxis(-pitch, right);
      Vector3f pitched = pitchQ.mult(offset);
      Vector3f dir = pitched.normalize();
      if (FastMath.abs(dir.y) < 0.98f) {
        offset = pitched;
      }
    }

    orbitRadius = offset.length();
    camera.setLocalTranslation(target.add(offset));
    camera.lookAt(target, Vector3f.UNIT_Y);
  }

  /**
   * Pan the camera in its local X/Y plane using pixel mouse deltas, and keep
   * the orbit target in sync so subsequent orbits stay around the new view
   * center.
   */
  protected void panCameraPixels(float dxPixels, float dyPixels) {
    if (camera == null || (dxPixels == 0f && dyPixels == 0f)) {
      return;
    }
    Quaternion rotation = camera.getLocalRotation();
    Vector3f right = rotation.getRotationColumn(0);
    Vector3f up = rotation.getRotationColumn(1);
    float dist = Math.max(camera.getWorldTranslation().subtract(getOrbitTarget()).length(), 1f);
    float scale = dist / 600f;
    Vector3f delta = right.mult(-dxPixels * scale).add(up.mult(dyPixels * scale));
    orbitPanOffset.addLocal(delta);
    camera.setLocalTranslation(camera.getLocalTranslation().add(delta));
  }

  /**
   * Pan the camera in its local X/Y plane and keep the orbit target in sync so
   * subsequent orbits stay around the new view center.
   */
  protected void panCamera(float x, float y) {
    panCameraPixels(x, y);
  }

  /**
   * Zoom toward or away from the orbit target. Positive {@code direction} zooms
   * out.
   */
  protected void zoomCamera(float direction) {
    if (camera == null) {
      return;
    }
    Vector3f target = getOrbitTarget();
    Vector3f offset = camera.getWorldTranslation().subtract(target);
    float dist = offset.length();
    if (dist < 1e-4f) {
      offset = camera.getLocalRotation().mult(Vector3f.UNIT_Z).mult(orbitRadius);
      dist = offset.length();
    }
    float factor = direction < 0f ? 0.92f : 1.08f;
    float newDist = FastMath.clamp(dist * factor, orbitMinDistance, orbitMaxDistance);
    offset.normalizeLocal().multLocal(newDist);
    orbitRadius = newDist;
    camera.setLocalTranslation(target.add(offset));
    camera.lookAt(target, Vector3f.UNIT_Y);
  }

  /**
   * A method to accept Computer Vision data (from OpenCV or BoofCv) and to
   * appropriately delegate it out to more specific methods
   * 
   * @param data
   *          cv data
   */
  public void onCvData(CVData data) {
    if (data == null) {
      return;
    }
    PointCloud pc = data.getPointCloud();
    if (pc != null) {
      onPointCloud(pc);
    }
  }

  @Override
  public void onJointAngles(Map<String, Double> angleMap) {
    for (String name : angleMap.keySet()) {
      ServiceInterface si = Runtime.getService(name);
      if (si instanceof ServoControl) {
        ((ServoControl) si).moveTo(angleMap.get(name));
      }
    }
  }

  @Override
  public void onPointCloud(PointCloud pc) {
    if (pc == null || app == null || assetManager == null) {
      return;
    }
    JMonkeyEngineConfig cfg = config;
    if (cfg != null && !cfg.depthCloud) {
      return;
    }
    if (cfg != null && cfg.depthRgbMesh) {
      return;
    }
    app.enqueue(() -> {
      updatePointCloudOnRenderThread(pc);
      return null;
    });
  }

  @Override
  public void onDepthFrame(DepthFrame frame) {
    if (frame == null || app == null || assetManager == null) {
      return;
    }
    JMonkeyEngineConfig cfg = config;
    if (cfg == null || !cfg.depthRgbMesh) {
      return;
    }
    if (!cfg.depthCloud) {
      return;
    }
    frame.decodeRgb();
    app.enqueue(() -> {
      updateRgbMeshOnRenderThread(frame);
      return null;
    });
  }

  @Override
  public void onDepthHud(DepthHud hud) {
    if (hud == null || hud.rgb == null || app == null || guiNode == null) {
      return;
    }
    JMonkeyEngineConfig cfg = config;
    if (cfg != null && !cfg.depthHud) {
      return;
    }
    app.enqueue(() -> {
      updateDepthHudOnRenderThread(hud);
      return null;
    });
  }

  /**
   * Create or move the dummy chest depth camera on the VinMoov torso. Safe from
   * any thread (enqueued to JME).
   */
  public void ensureChestDepthCamera() {
    if (app == null) {
      return;
    }
    JMonkeyEngineConfig cfg = (JMonkeyEngineConfig) config;
    app.enqueue(() -> {
      ensureChestDepthCameraOnRenderThread(cfg);
      return null;
    });
  }

  protected void updatePointCloudOnRenderThread(PointCloud pc) {
    ensureChestDepthCameraOnRenderThread((JMonkeyEngineConfig) config);
    syncDepthOverlayPose();
    Point3df[] points = pc.getData();
    int n = points == null ? 0 : points.length;
    if (pointCloudGeometry == null || pointCloudBuffer == null || n != pointCloudVertexCount) {
      initPointCloud(pc);
      return;
    }
    writePointCloudBuffers(pc);
    pointCloudMesh.setBuffer(VertexBuffer.Type.Position, 3, pointCloudBuffer);
    pointCloudMesh.setBuffer(VertexBuffer.Type.Color, 4, pointCloudColorBuffer);
    pointCloudMesh.updateBound();
    pointCloudMesh.updateCounts();
  }

  protected void ensureChestDepthCameraOnRenderThread(JMonkeyEngineConfig cfg) {
    if (cfg == null || rootNode == null || assetManager == null) {
      return;
    }
    ensureDepthOverlayNode();
    String nodeName = cfg.chestCameraNode;
    if (nodeName == null || nodeName.isEmpty()) {
      nodeName = "i01.chest.depthCamera";
    }
    Node parent = resolveChestCameraParent(cfg);
    Spatial existing = find(nodeName);
    Node camNode;
    if (existing instanceof Node) {
      camNode = (Node) existing;
      if (camNode.getParent() != parent) {
        parent.attachChild(camNode);
      }
      detachOverlayGeometryFromCharacter(camNode);
    } else {
      camNode = new Node(nodeName);
      camNode.setShadowMode(ShadowMode.Off);
      parent.attachChild(camNode);
      Box body = new Box(0.022f, 0.012f, 0.008f);
      Geometry geo = new Geometry(nodeName + ".body", body);
      Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
      mat.setColor("Color", ColorRGBA.Cyan);
      geo.setMaterial(mat);
      geo.setShadowMode(ShadowMode.Off);
      camNode.attachChild(geo);
    }
    Vector3f local = new Vector3f(cfg.chestCameraX, cfg.chestCameraY, cfg.chestCameraZ);
    if (cfg.chestCameraCenterOnTorso) {
      Node boundSrc = parent != rootNode ? parent : findRobotRoot(cfg);
      if (boundSrc != null) {
        Vector3f center = cachedTorsoCenter(boundSrc);
        if (boundSrc == parent) {
          local.addLocal(center);
        } else {
          local = boundSrc.localToWorld(center, null).addLocal(local);
        }
      }
    }
    camNode.setLocalTranslation(local);
    camNode.setLocalRotation(new Quaternion().fromAngles(cfg.chestCameraPitchDeg * FastMath.DEG_TO_RAD,
        cfg.chestCameraYawDeg * FastMath.DEG_TO_RAD, cfg.chestCameraRollDeg * FastMath.DEG_TO_RAD));
    chestDepthCameraNode = camNode;
    syncDepthOverlayPose();
    String parentName = parent.getName();
    if (parentName != null && !parentName.equals(lastChestParentName)) {
      lastChestParentName = parentName;
      Vector3f world = camNode.getWorldTranslation();
      log.info("Chest depth camera '{}' parented to {} at local {} world {}", nodeName, parentName, local, world);
    }
  }

  private void ensureDepthOverlayNode() {
    if (rootNode == null) {
      return;
    }
    if (depthOverlayNode == null) {
      Spatial existing = find("_mrl.depthOverlay");
      if (existing instanceof Node) {
        depthOverlayNode = (Node) existing;
      } else {
        depthOverlayNode = new Node("_mrl.depthOverlay");
        rootNode.attachChild(depthOverlayNode);
      }
      depthOverlayNode.setShadowMode(ShadowMode.Off);
    }
    boolean hasFrustum = false;
    boolean hasMeter = false;
    for (Spatial child : depthOverlayNode.getChildren()) {
      String n = child.getName();
      if (n != null && n.endsWith(".frustum")) {
        hasFrustum = true;
      }
      if ("_mrl.depthMeter".equals(n)) {
        hasMeter = true;
      }
    }
    if (!hasFrustum) {
      attachDepthFrustum(depthOverlayNode);
    }
    JMonkeyEngineConfig cfg = config;
    if (cfg == null || cfg.depthMeterStick) {
      if (!hasMeter) {
        attachDepthMeterStick(depthOverlayNode);
      }
    } else if (hasMeter) {
      Spatial meter = depthOverlayNode.getChild("_mrl.depthMeter");
      if (meter != null) {
        meter.removeFromParent();
      }
    }
    if (pointCloudGeometry != null && pointCloudGeometry.getParent() != depthOverlayNode) {
      depthOverlayNode.attachChild(pointCloudGeometry);
    }
    if (depthMeshGeometry != null && depthMeshGeometry.getParent() != depthOverlayNode) {
      depthOverlayNode.attachChild(depthMeshGeometry);
    }
  }

  private void syncDepthOverlayPose() {
    if (depthOverlayNode == null || chestDepthCameraNode == null) {
      return;
    }
    depthOverlayNode.setLocalTranslation(chestDepthCameraNode.getWorldTranslation());
    depthOverlayNode.setLocalRotation(chestDepthCameraNode.getWorldRotation());
    depthOverlayNode.setLocalScale(1f);
    lastChestCameraWorld.set(chestDepthCameraNode.getWorldTranslation());
  }

  /**
   * Old sessions parented the voxel mesh and frustum under VinMoov — pull them
   * off so Lighting/PBR on the character is not batched with this overlay.
   */
  private void detachOverlayGeometryFromCharacter(Node camNode) {
    if (camNode == null) {
      return;
    }
    List<Spatial> move = new ArrayList<>();
    for (Spatial child : camNode.getChildren()) {
      String n = child.getName() == null ? "" : child.getName();
      if (n.contains("depthCloud") || n.contains("depthRgbMesh") || n.endsWith(".frustum") || n.contains("depthMeter")) {
        move.add(child);
      }
    }
    for (Spatial child : move) {
      child.removeFromParent();
      if (depthOverlayNode != null) {
        depthOverlayNode.attachChild(child);
      }
    }
  }

  private Vector3f cachedTorsoCenter(Node boundSrc) {
    String key = boundSrc.getName();
    if (cachedTorsoCenterLocal != null && key != null && key.equals(cachedTorsoCenterParent)) {
      return cachedTorsoCenterLocal;
    }
    cachedTorsoCenterLocal = torsoCenterInParent(boundSrc);
    cachedTorsoCenterParent = key;
    return cachedTorsoCenterLocal;
  }

  private Node resolveChestCameraParent(JMonkeyEngineConfig cfg) {
    String robot = inferRobotNameFromNodeConfig(cfg);
    if (robot == null || robot.isEmpty()) {
      robot = "i01";
    }
    LinkedHashSet<String> names = new LinkedHashSet<>();
    if (cfg.chestCameraParent != null && !cfg.chestCameraParent.isEmpty()) {
      names.add(cfg.chestCameraParent);
    }
    names.add(robot + ".torso.topStom");
    names.add(robot + ".torso.midStom");
    names.add(robot + ".torso.lowStom");
    names.add(robot);
    for (String name : names) {
      Spatial found = find(name);
      if (found instanceof Node) {
        return (Node) found;
      }
      if (found != null && found.getParent() != null) {
        return found.getParent();
      }
    }
    log.warn("Chest depth camera parent not found (tried {}) — attaching to root", names);
    return rootNode;
  }

  private Node findRobotRoot(JMonkeyEngineConfig cfg) {
    String robot = inferRobotNameFromNodeConfig(cfg);
    if (robot == null || robot.isEmpty()) {
      robot = "i01";
    }
    Spatial s = find(robot);
    return s instanceof Node ? (Node) s : null;
  }

  /**
   * Visual center of the torso mesh in {@code parent}'s local frame. Skips
   * head/arm/hand subtrees so a topStom node that also owns the limbs does not
   * pull the camera up into the neck.
   */
  private Vector3f torsoCenterInParent(Node parent) {
    BoundingBox acc = null;
    ArrayList<Geometry> meshes = new ArrayList<>();
    collectTorsoGeometries(parent, meshes, true);
    for (Geometry g : meshes) {
      BoundingVolume bv = g.getWorldBound();
      if (bv instanceof BoundingBox) {
        if (acc == null) {
          acc = new BoundingBox((BoundingBox) bv);
        } else {
          acc.mergeLocal(bv);
        }
      }
    }
    if (acc == null && parent.getWorldBound() instanceof BoundingBox) {
      acc = new BoundingBox((BoundingBox) parent.getWorldBound());
    }
    if (acc == null) {
      return Vector3f.ZERO.clone();
    }
    return parent.worldToLocal(acc.getCenter(), null);
  }

  private void collectTorsoGeometries(Spatial spatial, List<Geometry> out, boolean root) {
    if (spatial == null) {
      return;
    }
    String name = spatial.getName() == null ? "" : spatial.getName().toLowerCase();
    if (!root && (name.contains("head") || name.contains("arm") || name.contains("hand") || name.contains("eye")
        || name.contains("depthcamera") || name.contains("depthcloud") || name.contains("depthrgb"))) {
      return;
    }
    if (spatial instanceof Geometry) {
      out.add((Geometry) spatial);
      return;
    }
    if (spatial instanceof Node) {
      for (Spatial child : ((Node) spatial).getChildren()) {
        collectTorsoGeometries(child, out, false);
      }
    }
  }

  private void attachDepthFrustum(Node camNode) {
    float z = 0.55f;
    float hw = 0.28f;
    float hh = 0.21f;
    Vector3f o = Vector3f.ZERO;
    Vector3f[] c = { new Vector3f(-hw, -hh, z), new Vector3f(hw, -hh, z), new Vector3f(hw, hh, z),
        new Vector3f(-hw, hh, z) };
    Vector3f[] verts = new Vector3f[16];
    int i = 0;
    for (int k = 0; k < 4; k++) {
      verts[i++] = o;
      verts[i++] = c[k];
    }
    for (int k = 0; k < 4; k++) {
      verts[i++] = c[k];
      verts[i++] = c[(k + 1) % 4];
    }
    Mesh mesh = new Mesh();
    mesh.setMode(Mesh.Mode.Lines);
    mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verts));
    mesh.updateBound();
    Geometry g = new Geometry(camNode.getName() + ".frustum", mesh);
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", new ColorRGBA(0.2f, 0.9f, 1f, 1f));
    g.setMaterial(mat);
    camNode.attachChild(g);
  }

  /**
   * 1 m along camera +Z with 10 cm ticks. Same world meters as IK — if a
   * known object disagrees with this stick, use {@link #calibrateDepthScale}.
   */
  private void attachDepthMeterStick(Node overlay) {
    Vector3f[] verts = new Vector3f[22];
    int i = 0;
    verts[i++] = Vector3f.ZERO;
    verts[i++] = new Vector3f(0f, 0f, 1f);
    for (int t = 1; t <= 10; t++) {
      float z = t * 0.1f;
      float tick = t == 10 ? 0.04f : 0.02f;
      verts[i++] = new Vector3f(-tick, 0f, z);
      verts[i++] = new Vector3f(tick, 0f, z);
    }
    Mesh mesh = new Mesh();
    mesh.setMode(Mesh.Mode.Lines);
    mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verts));
    mesh.updateBound();
    Geometry g = new Geometry("_mrl.depthMeter", mesh);
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", new ColorRGBA(1f, 0.85f, 0.15f, 1f));
    g.setMaterial(mat);
    overlay.attachChild(g);
  }

  protected void updateDepthHudOnRenderThread(DepthHud hud) {
    int w = hud.width;
    int h = hud.height;
    if (w <= 0 || h <= 0 || hud.rgb == null || hud.rgb.length < w * h * 3) {
      return;
    }
    ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 3);
    for (int y = 0; y < h; y++) {
      int src = (h - 1 - y) * w * 3;
      buf.put(hud.rgb, src, w * 3);
    }
    buf.flip();
    Image image = new Image(Image.Format.RGB8, w, h, buf, ColorSpace.sRGB);
    if (depthHudTexture == null || depthHudGeometry == null) {
      depthHudTexture = new Texture2D(image);
      depthHudTexture.setMagFilter(Texture.MagFilter.Nearest);
      depthHudTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
      depthHudMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
      depthHudMat.setTexture("ColorMap", depthHudTexture);
      Quad q = new Quad(320, 240);
      depthHudGeometry = new Geometry("depthHud", q);
      depthHudGeometry.setMaterial(depthHudMat);
      depthHudGeometry.setLocalTranslation(16, 16, 0);
      guiNode.attachChild(depthHudGeometry);
    } else {
      depthHudTexture.setImage(image);
    }
  }

  public void onRegistered(Registration registration) {
    try {
      log.info("{}.onRegistered({})", getName(), registration);
      if (registration.getName().contentEquals("i01.head.jaw")) {
        log.info("here");
      }
      if (autoAttach) {
        attach(registration.getFullName());
      }
    } catch (Exception e) {
      error(e);
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
   *          t
   * @param x
   *          coordinate
   * @param y
   *          coordinate
   * @param color
   *          c
   * @param size
   *          s
   * 
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

  public void setShowHandPositions(boolean show) {
    this.showHandPositions = show;
    if (!show) {
      removeHandPositionOverlays();
    }
  }

  public boolean getShowHandPositions() {
    return showHandPositions;
  }

  public void setHandPositionRobot(String robotName) {
    this.handPositionRobot = robotName;
  }

  public String getHandPositionRobot() {
    return handPositionRobot;
  }

  public void setHandMarkerRadius(float radius) {
    this.handMarkerRadius = radius;
    // recreate markers next frame with new size
    detachHandMarker(leftHandMarker);
    detachHandMarker(rightHandMarker);
    leftHandMarker = null;
    rightHandMarker = null;
  }

  /**
   * Upper-left HUD + colored dots at InMoov left/right hand world positions.
   * Blue = left, red = right. Called each frame from {@link #simpleUpdate(float)}.
   * Positioned from the GUI camera so a resize or DPI scale cannot drop it
   * onto the OAK-D heatmap in the lower-left.
   */
  protected void updateHandPositionHud() {
    if (!showHandPositions || app == null || rootNode == null) {
      return;
    }

    String robot = handPositionRobot;
    if (robot == null || robot.isEmpty()) {
      robot = inferRobotNameFromNodeConfig((JMonkeyEngineConfig) config);
      if (robot == null) {
        robot = "i01";
      }
    }

    Spatial leftHand = findHandSpatial(robot, "left");
    Spatial rightHand = findHandSpatial(robot, "right");
    Vector3f left = leftHand != null ? leftHand.getWorldTranslation() : null;
    Vector3f right = rightHand != null ? rightHand.getWorldTranslation() : null;
    Vector3f ikLeft = ikLeftHandWorld;

    if (guiNode != null) {
      String err = formatHudError(left, ikLeft);
      String text = String.format("L hand: %s\nR hand: %s\nL IK:   %s\nL goal: %s%s%s%s", formatHudVec(left), formatHudVec(right), formatHudVec(ikLeftHandCurrent),
          formatHudVec(ikLeftHandGoal), err != null ? "\nL err:  " + err : "", depthHudLine(), reachHudLine());
      ensureHandPositionHudText();
      if (handPositionHudText != null) {
        handPositionHudText.setText(text);
        float guiH = guiHudHeight();
        // BitmapText origin is the top-left of the block; GUI Y=0 is the bottom.
        handPositionHudText.setLocalTranslation(16f, guiH - 12f, 1f);
      }
    }

    leftHandMarker = ensureHandMarker(leftHandMarker, LEFT_HAND_MARKER, ColorRGBA.Blue);
    rightHandMarker = ensureHandMarker(rightHandMarker, RIGHT_HAND_MARKER, ColorRGBA.Red);
    ikLeftHandMarker = ensureHandMarker(ikLeftHandMarker, IK_LEFT_HAND_MARKER, ColorRGBA.Green);
    syncHandMarker(leftHandMarker, left);
    syncHandMarker(rightHandMarker, right);
    syncHandMarker(ikLeftHandMarker, ikLeft);
  }

  private void ensureHandPositionHudText() {
    // Drop the old HudText path (it was anchored from the bottom / AppSettings
    // height and sat under the depth heatmap).
    HudText legacy = guiText.remove(HAND_POSITION_HUD_KEY);
    if (legacy != null && legacy.getNode() != null && legacy.getNode().getParent() != null) {
      legacy.getNode().removeFromParent();
    }
    if (handPositionHudText != null && handPositionHudText.getParent() != null) {
      return;
    }
    if (app == null || guiNode == null) {
      return;
    }
    BitmapFont font = app.loadGuiFont();
    handPositionHudText = new BitmapText(font, false);
    float size = fontSize > 0 ? fontSize : font.getCharSet().getRenderedSize();
    handPositionHudText.setSize(size);
    handPositionHudText.setColor(ColorRGBA.Yellow);
    handPositionHudText.setQueueBucket(Bucket.Gui);
    handPositionHudText.setCullHint(CullHint.Never);
    guiNode.attachChild(handPositionHudText);
    log.info("IK HUD attached at upper-left of GUI viewport ({} px tall)", guiHudHeight());
  }

  /**
   * Live GUI framebuffer height. {@link AppSettings#getHeight()} stays at the
   * launch resolution when the window is resized.
   */
  private float guiHudHeight() {
    if (app != null && app.getGuiViewPort() != null && app.getGuiViewPort().getCamera() != null) {
      return app.getGuiViewPort().getCamera().getHeight();
    }
    if (cam != null) {
      return cam.getHeight();
    }
    return settings != null ? settings.getHeight() : height;
  }

  private void removeHandPositionOverlays() {
    HudText hud = guiText.remove(HAND_POSITION_HUD_KEY);
    if (hud != null && hud.getNode() != null && hud.getNode().getParent() != null) {
      hud.getNode().removeFromParent();
    }
    if (handPositionHudText != null && handPositionHudText.getParent() != null) {
      handPositionHudText.removeFromParent();
    }
    handPositionHudText = null;
    detachHandMarker(leftHandMarker);
    detachHandMarker(rightHandMarker);
    detachHandMarker(ikLeftHandMarker);
    leftHandMarker = null;
    rightHandMarker = null;
    ikLeftHandMarker = null;
  }

  private Geometry ensureHandMarker(Geometry existing, String name, ColorRGBA color) {
    if (existing != null && existing.getParent() != null) {
      return existing;
    }
    if (assetManager == null || rootNode == null) {
      return existing;
    }
    Sphere sphere = new Sphere(12, 12, handMarkerRadius);
    Geometry marker = new Geometry(name, sphere);
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", color);
    // Keep dots readable when briefly occluded by fingers/geometry
    mat.getAdditionalRenderState().setDepthTest(true);
    mat.getAdditionalRenderState().setDepthWrite(true);
    marker.setMaterial(mat);
    marker.setCullHint(CullHint.Never);
    rootNode.attachChild(marker);
    return marker;
  }

  private void syncHandMarker(Geometry marker, Vector3f worldPos) {
    if (marker == null) {
      return;
    }
    if (worldPos == null) {
      marker.setCullHint(CullHint.Always);
      return;
    }
    marker.setCullHint(CullHint.Never);
    marker.setLocalTranslation(worldPos);
  }

  private void detachHandMarker(Geometry marker) {
    if (marker != null && marker.getParent() != null) {
      marker.removeFromParent();
    }
  }

  private Spatial findHandSpatial(String robot, String side) {
    // Prefer wrist (configured InMoov hand root); fall back to common aliases
    String[] candidates = new String[] {
        robot + "." + side + "Hand.wrist",
        robot + "." + side + "Hand",
        robot + "." + side + "Arm.hand",
        side + "Hand.wrist",
        side + "Hand"
    };
    for (String name : candidates) {
      Spatial spatial = find(name);
      if (spatial != null) {
        return spatial;
      }
    }
    return null;
  }

  private static String formatHudVec(Vector3f v) {
    if (v == null) {
      return "n/a";
    }
    return String.format("%.3f, %.3f, %.3f", v.x, v.y, v.z);
  }

  private String depthHudLine() {
    float s = getDepthCloudScale();
    if (lastClickDistanceM > 1e-4f) {
      return String.format("\ndepth ×%.3f  click %.3f m", s, lastClickDistanceM);
    }
    return String.format("\ndepth ×%.3f  click —", s);
  }

  private String reachHudLine() {
    if (config == null || !config.reachCloud || lastReachCloud == null || lastReachCloud.size() == 0) {
      return "";
    }
    return String.format("\nL reach: %d pts", lastReachCloud.size());
  }

  private static String formatHudError(Vector3f sim, Vector3f ik) {
    if (sim == null || ik == null) {
      return null;
    }
    return String.format("%.3f m", sim.distance(ik));
  }

  /**
   * Thread-safe world translation of a named spatial (meters, JME Y-up).
   *
   * @param name
   *          node name, e.g. {@code i01.leftArm.omoplate}
   * @return world position, or null if missing / JME not started
   */
  public Point3df getWorldTranslation(String name) {
    if (app == null) {
      log.warn("getWorldTranslation({}) — JME app not started", name);
      return null;
    }
    try {
      Future<Point3df> future = app.enqueue(() -> {
        Spatial spatial = find(name);
        if (spatial == null) {
          return null;
        }
        Vector3f v = spatial.getWorldTranslation();
        return new Point3df(v.x, v.y, v.z);
      });
      return future.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("getWorldTranslation({}) failed", name, e);
      return null;
    }
  }

  /**
   * Thread-safe 4x4 world transform of a named spatial (meters, JME Y-up). Used
   * as the InverseKinematics3D DH base so the arm chain shares the JME root
   * origin.
   */
  public Matrix getWorldMatrix(String name) {
    if (app == null) {
      log.warn("getWorldMatrix({}) — JME app not started", name);
      return null;
    }
    try {
      Future<Matrix> future = app.enqueue(() -> {
        Spatial spatial = find(name);
        if (spatial == null) {
          return null;
        }
        com.jme3.math.Vector3f t = spatial.getWorldTranslation();
        com.jme3.math.Matrix3f r = spatial.getWorldRotation().toRotationMatrix();
        Matrix m = Matrix.identity(4);
        for (int row = 0; row < 3; row++) {
          for (int col = 0; col < 3; col++) {
            m.elements[row][col] = r.get(row, col);
          }
        }
        m.elements[0][3] = t.x;
        m.elements[1][3] = t.y;
        m.elements[2][3] = t.z;
        return m;
      });
      return future.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("getWorldMatrix({}) failed", name, e);
      return null;
    }
  }

  /**
   * Fired after VinMoov is bound and node mappers are applied so InverseKinematics3D
   * can sample the omoplate world pose.
   */
  public String publishSceneReady(String name) {
    return name;
  }

  /**
   * Thread-safe snapshot of several spatial world translations (meters, JME
   * Y-up). Missing names are omitted from the result.
   */
  public Map<String, Point3df> getWorldTranslations(String... names) {
    Map<String, Point3df> empty = new LinkedHashMap<>();
    if (app == null || names == null) {
      log.warn("getWorldTranslations — JME app not started or no names");
      return empty;
    }
    try {
      Future<Map<String, Point3df>> future = app.enqueue(() -> {
        Map<String, Point3df> out = new LinkedHashMap<>();
        for (String name : names) {
          if (name == null) {
            continue;
          }
          Spatial spatial = find(name);
          if (spatial == null) {
            continue;
          }
          Vector3f v = spatial.getWorldTranslation();
          out.put(name, new Point3df(v.x, v.y, v.z));
        }
        return out;
      });
      return future.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("getWorldTranslations failed", e);
      return empty;
    }
  }

  /**
   * The named nodes' rotation axes, axis origins, current joint angles and
   * servo&rarr;mesh maps, sampled together on the render thread.
   *
   * <p>
   * This is the measurement a kinematic solver needs: it fully describes each
   * revolute joint of the rig, so a chain built from it reproduces the simulated
   * arm for <em>any</em> joint angles. Missing nodes are skipped, so check the
   * size of the result.
   * </p>
   *
   * @param names
   *          node names ordered parent to child, e.g.
   *          {@code i01.leftArm.omoplate ... i01.leftArm.bicep}
   */
  public List<JointFrame> getJointFrames(String... names) {
    List<JointFrame> frames = new ArrayList<>();
    if (app == null || names == null) {
      log.warn("getJointFrames — JME app not started or no names");
      return frames;
    }
    try {
      Future<List<JointFrame>> future = app.enqueue(() -> {
        List<JointFrame> out = new ArrayList<>();
        for (String name : names) {
          if (name == null) {
            continue;
          }
          UserData data = getUserData(name);
          double[] axis = util.getWorldJointAxis(name);
          if (data == null || axis == null) {
            log.warn("getJointFrames - no node / rotation axis for {}", name);
            continue;
          }
          JointFrame frame = new JointFrame(name, new Point(axis[0], axis[1], axis[2]), new Point(axis[3], axis[4], axis[5]));
          frame.rotationMask = data.rotationMask;
          frame.angleDeg = data.getCurrentAngleDeg();
          if (data.mapper != null) {
            frame.withServoMap(data.mapper.getMinX(), data.mapper.getMaxX(), data.mapper.getMinY(), data.mapper.getMaxY());
          }
          out.add(frame);
        }
        return out;
      });
      return future.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("getJointFrames failed", e);
      return frames;
    }
  }

  /**
   * {@link #getJointFrames(String...)} for one InMoov arm, in chain order.
   */
  public List<JointFrame> getArmJointFrames(String robot, String side) {
    String prefix = robot + "." + side + "Arm.";
    return getJointFrames(prefix + "omoplate", prefix + "shoulder", prefix + "rotate", prefix + "bicep");
  }

  /**
   * Omoplate, shoulder, rotate, bicep, and wrist world translations for one
   * InMoov arm (meters).
   */
  public Map<String, Point3df> getArmChainWorldTranslations(String robot, String side) {
    Map<String, Point3df> chain = getWorldTranslations(robot + "." + side + "Arm.omoplate", robot + "." + side + "Arm.shoulder", robot + "." + side + "Arm.rotate",
        robot + "." + side + "Arm.bicep", robot + "." + side + "Hand.wrist");
    if (!chain.containsKey(robot + "." + side + "Hand.wrist")) {
      Point3df hand = getHandWorldTranslation(robot, side);
      if (hand != null) {
        chain.put(robot + "." + side + "Hand.wrist", hand);
      }
    }
    return chain;
  }

  /**
   * Thread-safe world translation of an InMoov hand / wrist node.
   */
  public Point3df getHandWorldTranslation(String robot, String side) {
    if (app == null) {
      log.warn("getHandWorldTranslation({}, {}) — JME app not started", robot, side);
      return null;
    }
    try {
      Future<Point3df> future = app.enqueue(() -> {
        Spatial spatial = findHandSpatial(robot, side);
        if (spatial == null) {
          return null;
        }
        Vector3f v = spatial.getWorldTranslation();
        return new Point3df(v.x, v.y, v.z);
      });
      return future.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("getHandWorldTranslation({}, {}) failed", robot, side, e);
      return null;
    }
  }

  /**
   * InverseKinematics3D / Fabrik {@code publishIkGoal} callback. Green marker is
   * the Cartesian goal the solver is walking toward.
   */
  public void onIkGoal(Point position) {
    if (position == null) {
      ikLeftHandGoal = null;
    } else {
      ikLeftHandGoal = new Vector3f((float) position.getX(), (float) position.getY(), (float) position.getZ());
    }
    refreshIkMarker();
  }

  /**
   * InverseKinematics3D {@code publishWorldPosition} callback. Updates the IK
   * palm HUD; green stays on the goal when one is active.
   */
  public void onWorldPosition(Point position) {
    if (position == null) {
      ikLeftHandCurrent = null;
    } else {
      ikLeftHandCurrent = new Vector3f((float) position.getX(), (float) position.getY(), (float) position.getZ());
    }
    refreshIkMarker();
  }

  /**
   * Show a green marker + HUD line at the IK goal (or current palm if no goal).
   * Pass nulls to hide.
   */
  public void setIkHandWorldPosition(Double x, Double y, Double z) {
    if (x == null || y == null || z == null) {
      ikLeftHandCurrent = null;
      ikLeftHandGoal = null;
      refreshIkMarker();
      return;
    }
    ikLeftHandCurrent = new Vector3f(x.floatValue(), y.floatValue(), z.floatValue());
    refreshIkMarker();
  }

  private void refreshIkMarker() {
    ikLeftHandWorld = ikLeftHandGoal != null ? ikLeftHandGoal : ikLeftHandCurrent;
  }

  /**
   * Show or hide the cyan left-hand reach cloud. When turning on with no cloud
   * yet, sample VinMoov's left arm (omoplate → wrist) via forward kinematics.
   */
  public boolean setReachCloud(boolean show) {
    if (config == null) {
      return false;
    }
    if (!show) {
      config.reachCloud = false;
      setReachCloudVisible(false);
      broadcastState();
      return false;
    }
    PointCloud sampled = sampleLeftHandReachFromVinMoov();
    if (sampled != null && sampled.size() > 0) {
      lastReachCloud = sampled;
    }
    if (lastReachCloud == null || lastReachCloud.size() == 0) {
      error("Left-hand reach cloud needs VinMoov loaded (or a FABRIK chain)");
      config.reachCloud = false;
      broadcastState();
      return false;
    }
    config.reachCloud = true;
    onReachCloud(lastReachCloud);
    broadcastState();
    return true;
  }

  public boolean getReachCloud() {
    return config != null && config.reachCloud;
  }

  /**
   * FABRIK {@code publishReachCloud} — world-meter palm samples, Y-up.
   */
  public void onReachCloud(PointCloud pc) {
    lastReachCloud = pc;
    reachCloudPointCount = pc == null ? 0 : pc.size();
    if (pc == null || pc.size() == 0) {
      if (config != null) {
        config.reachCloud = false;
      }
      setReachCloudVisible(false);
      return;
    }
    if (config != null) {
      config.reachCloud = true;
    }
    if (app == null || assetManager == null) {
      return;
    }
    app.enqueue(() -> {
      updateReachCloudOnRenderThread(pc);
      return null;
    });
    broadcastState();
  }

  public void setReachCloudVisible(boolean show) {
    if (config != null) {
      config.reachCloud = show;
    }
    if (app == null) {
      return;
    }
    app.enqueue(() -> {
      if (reachCloudGeometry != null) {
        reachCloudGeometry.setCullHint(show ? CullHint.Never : CullHint.Always);
      }
      return null;
    });
  }

  /**
   * Measure the live VinMoov left arm and sample its palm workspace. Safe from
   * the service thread (joint frames are copied off the render thread).
   */
  public PointCloud sampleLeftHandReachFromVinMoov() {
    String robot = handPositionRobot;
    if (robot == null || robot.isEmpty()) {
      robot = inferRobotNameFromNodeConfig(config);
      if (robot == null) {
        robot = "i01";
      }
    }
    JMonkeyEngineConfig cfg = config;
    int steps = cfg != null && cfg.reachCloudSteps > 1 ? cfg.reachCloudSteps : ReachCloud.DEFAULT_STEPS;
    float voxel = cfg != null && cfg.reachCloudVoxelM > 0f ? cfg.reachCloudVoxelM : ReachCloud.DEFAULT_VOXEL_M;
    List<JointFrame> frames = getArmJointFrames(robot, "left");
    if (frames.size() < 4) {
      log.warn("sampleLeftHandReachFromVinMoov — measured {} of 4 left-arm joints", frames.size());
      return null;
    }
    Point3df wrist = getHandWorldTranslation(robot, "left");
    if (wrist == null) {
      log.warn("sampleLeftHandReachFromVinMoov — no left wrist node");
      return null;
    }
    PointCloud cloud = ReachCloud.sample(frames, new Point(wrist.x, wrist.y, wrist.z), steps, voxel);
    reachCloudPointCount = cloud.size();
    log.info("Left-hand reach cloud {} voxels from VinMoov {} ({} joint steps, {} m cells)", cloud.size(), robot, steps, voxel);
    return cloud;
  }

  protected void updateReachCloudOnRenderThread(PointCloud pc) {
    Point3df[] points = pc == null ? null : pc.getData();
    int n = points == null ? 0 : points.length;
    if (reachCloudGeometry == null || reachCloudBuffer == null || n != reachCloudVertexCount) {
      initReachCloud(pc);
      return;
    }
    writeReachCloudBuffers(pc);
    reachCloudMesh.setBuffer(VertexBuffer.Type.Position, 3, reachCloudBuffer);
    reachCloudMesh.setBuffer(VertexBuffer.Type.Color, 4, reachCloudColorBuffer);
    reachCloudMesh.updateBound();
    reachCloudMesh.updateCounts();
    reachCloudGeometry.setCullHint(CullHint.Never);
  }

  private void initReachCloud(PointCloud pc) {
    Point3df[] points = pc == null ? null : pc.getData();
    int n = points == null ? 0 : points.length;
    reachCloudVertexCount = n;
    int verts = Math.max(1, n) * 8;
    reachCloudBuffer = BufferUtils.createFloatBuffer(verts * 3);
    reachCloudColorBuffer = BufferUtils.createFloatBuffer(verts * 4);
    writeReachCloudBuffers(pc);

    reachCloudMesh = new Mesh();
    reachCloudMesh.setMode(Mesh.Mode.Triangles);
    reachCloudMesh.setBuffer(VertexBuffer.Type.Position, 3, reachCloudBuffer);
    reachCloudMesh.setBuffer(VertexBuffer.Type.Color, 4, reachCloudColorBuffer);
    reachCloudMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(DepthCloudJme.cubeIndices(Math.max(1, n))));
    reachCloudMesh.updateBound();
    reachCloudMesh.updateCounts();

    if (reachCloudGeometry != null) {
      reachCloudGeometry.removeFromParent();
    }
    reachCloudGeometry = new Geometry(DepthPick.LEFT_HAND_REACH, reachCloudMesh);
    reachCloudMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    reachCloudMat.setBoolean("VertexColor", true);
    reachCloudMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
    reachCloudMat.getAdditionalRenderState().setDepthWrite(false);
    reachCloudMat.getAdditionalRenderState().setDepthTest(true);
    reachCloudGeometry.setMaterial(reachCloudMat);
    reachCloudGeometry.setShadowMode(ShadowMode.Off);
    reachCloudGeometry.setQueueBucket(Bucket.Transparent);
    reachCloudGeometry.setCullHint(CullHint.Never);
    if (rootNode != null) {
      rootNode.attachChild(reachCloudGeometry);
    }
  }

  private void writeReachCloudBuffers(PointCloud pc) {
    Point3df[] points = pc == null ? null : pc.getData();
    float[] colors = pc == null ? null : pc.getColors();
    int n = points == null ? 0 : points.length;
    JMonkeyEngineConfig cfg = config;
    float voxel = cfg != null ? cfg.reachCloudVoxelM : ReachCloud.DEFAULT_VOXEL_M;
    float half = Math.max(0.006f, voxel * 0.45f);

    reachCloudBuffer.clear();
    reachCloudColorBuffer.clear();
    float[] corners = new float[24];
    for (int i = 0; i < n; i++) {
      Point3df p = points[i];
      DepthCloudJme.worldVoxelCorners(p.x, p.y, p.z, half, corners);
      for (int c = 0; c < 24; c++) {
        reachCloudBuffer.put(corners[c]);
      }
      float r = ReachCloud.COLOR[0];
      float g = ReachCloud.COLOR[1];
      float b = ReachCloud.COLOR[2];
      float a = ReachCloud.COLOR[3];
      if (colors != null && colors.length >= (i + 1) * 4) {
        r = colors[i * 4];
        g = colors[i * 4 + 1];
        b = colors[i * 4 + 2];
        a = colors[i * 4 + 3];
      }
      for (int c = 0; c < 8; c++) {
        reachCloudColorBuffer.put(r).put(g).put(b).put(a);
      }
    }
    int pad = Math.max(1, n);
    for (int i = n; i < pad; i++) {
      for (int c = 0; c < 24; c++) {
        reachCloudBuffer.put(0f);
      }
      for (int c = 0; c < 8; c++) {
        reachCloudColorBuffer.put(0f).put(0f).put(0f).put(0f);
      }
    }
    reachCloudBuffer.flip();
    reachCloudColorBuffer.flip();
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
   * instant rotation on an particular axis
   * 
   * @param name
   *          reference name
   * @param axis
   *          axis
   * @param degrees
   *          degree
   * 
   */
  public void rotateOnAxis(String name, String axis, double degrees) {
    addMsg("rotateTo", name, axis, degrees);
  }

  /**
   * incremental movement on an axis with a speed
   * 
   * @param name
   *          name
   * @param axis
   *          a
   * @param degrees
   *          d
   * @param speed
   *          s
   * 
   */
  public void rotateOnAxis(String name, String axis, double degrees, double speed) {
    interpolator.addAnimation("rotateTo", name, axis, degrees, speed);
  }

  /**
   * rotate on the "default" axis to a location without using speed
   * 
   * @param name
   *          name to rotate
   * @param degrees
   *          amount to rotate
   * 
   */
  public void rotateTo(String name, double degrees) {
    addMsg("rotateTo", name, null, degrees);
  }

  /**
   * rotate on the "default" axis using speed
   * 
   * @param name
   *          name of joint
   * @param degrees
   *          amount to move
   * @param speed
   *          speed
   * 
   */
  public void rotateTo(String name, double degrees, double speed) {
    interpolator.addAnimation("rotateTo", name, null, degrees, speed);
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

  public void saveMsgs() throws IOException {
    List<Jme3Msg> temp = history;
    history = new ArrayList<Jme3Msg>();
    String data = CodecUtils.toJson(temp);
    FileIO.toFile(String.format("jme3-msg-history-%d.json", System.currentTimeMillis()), data);
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

  @Override
  public void sendRemote(Message msg) throws Exception {
    // TODO Auto-generated method stub

  }

  public void setDefaultServoSpeed(Double speed) {
    defaultServoSpeed = speed;
  }

  public void setDisplayFps(boolean b) {
    app.setDisplayFps(b);
  }

  public void setDisplayStatView(boolean b) {
    app.setDisplayStatView(b);
  }

  public void setFloorGrid(boolean b) {
    Spatial s = find("floor-grid");
    if (s == null) {
      addGrid("floor-grid");
      s = get("floor-grid");
    }
    if (b) {
      show("floor-grid");
    } else {
      hide("floor-grid");
    }
  }

  public void setFontColor(String color) {
    fontColor = color;
  }

  public void setFontSize(int size) {
    fontSize = size;
  }

  public Mapper setMapper(String name, int minx, int maxx, int miny, int maxy) {
    return setMapper(name, (double) minx, (double) maxx, (double) miny, (double) maxy);
  }

  public Mapper setMapper(String name, double minx, double maxx, double miny, double maxy) {
    UserData node = getUserData(name);
    if (node == null) {
      error("setMapper %s does not exist", name);
      return null;
    }
    node.mapper = new MapperLinear(minx, maxx, miny, maxy);
    return node.mapper;
  }

  public void setRotation(String name, double xRot, double yRot, double zRot) {
    addMsg("setRotation", name, xRot, yRot, zRot);
  }

  public void setRotation(String name, String axis) {
    UserData o = getUserData(name);
    if (o == null) {
      error("setRotation %s could not be found", name);
      return;
    }
    // WRONG !!!! - getLocalUnitVector
    // o.rotationMask = util.getUnitVector(rotation);
    // o.rotationMask = util.getLocalUnitVector(o.getSpatial(), rotation);
    o.rotationMask = axis;
  }

  @Deprecated
  public String publishSelected(String data) {
    return data;
  }

  // xxx
  public void setSelected(Spatial newSelected) {

    // turn off old
    if (selectedForView != null) {
      // enableBoundingBox(selectedForView, false);
      // enableAxes(selectedForView, false);

      // try to publish "quality" data
      // String[] parts = newSelected.getUserData("ORIGINAL_PATH");
    }

    // set selected
    selectedForView = newSelected;

    // send the movement utility info on the current selected item & current
    // view
    // so that it can update the view with changes on the item
    // TODO - optimize for when there is no view
    util.setSelectedForView(selectedForView);

    // turn on new
    if (newSelected != null) {
      // enableBoundingBox(newSelected, true);
      // enableAxes(newSelected, true);

      String originalPath = newSelected.getUserData("ORIGINAL_PATH");
      // invoke("publishSelected", originalPath);

      // invoke("getSelected");
      if (originalPath != null) {
        selectedPath = originalPath;

        // Kludge ... this should be structured and set directly on the data
        // when building the inmoov model
        // but in an attempt to improve data quality we got to do this matching
        // thing ..
        String normalizedPath = findCommonPrefix(originalPath);

        if (normalizedPath != null) {
          invoke("setSelectedPath", normalizedPath);
        }
      }

    }

  }

  /**
   * horrific function to calculate hits on path parts :/ to improve path
   * selection
   * 
   * @param path
   * @return
   */
  public String findCommonPrefix(String path) {
    // found in nodes
    ArrayList<String> pathParts = new ArrayList<>(Arrays.asList(path.split("/")));
    Collections.reverse(pathParts);
    for (String part : pathParts) {
      if (nodes.containsKey(part)) {

        // i01.leftHand.index3
        if (Character.isDigit(part.charAt(part.length() - 1))) {
          part = part.substring(0, part.length() - 1);
        }

        return part;
      }
    }
    return null;
  }

  public void setSelected(String name) {
    Spatial s = get(name);
    if (s == null) {
      log.error("setSelected {} is null", name);
      return;
    }
    setSelected(s);
  }

  public void setTransform(String name, double x, double y, double z, double xRot, double yRot, double zRot) {
    addMsg("setTransform", name, x, y, z, xRot, yRot, zRot);
  }

  public void setTranslation(String name, double x, double y, double z) {
    addMsg("setTranslation", name, x, y, z);
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

  public void setVisible(String name, boolean visible) {
    Spatial s = get(name);
    if (visible) {
      s.setCullHint(CullHint.Never);
    } else {
      s.setCullHint(CullHint.Always);
    }
  }

  public void show(String name) {
    setVisible(name, true);
  }

  public void showMenu(boolean b) {
    // TODO - implement !!!
  }

  transient BulletAppState bulletAppState;

  private boolean usePhysics;

  transient private Thread mainThread;

  public void simpleInitApp() {

    stateManager = app.getStateManager();

    if (usePhysics) {
      bulletAppState = new BulletAppState();
      stateManager.attach(bulletAppState);
    }

    setDisplayFps(false);

    setDisplayStatView(false);

    assetManager = app.getAssetManager();

    inputManager = app.getInputManager();

    guiNode = app.getGuiNode();
    
    cam = app.getCamera();
    rootNode = app.getRootNode();
    rootNode.setName(ROOT);
    rootNode.attachChild(camera);

    viewPort = app.getViewPort();
    // Setting the direction to Spatial to camera, this means the camera will
    // copy the movements of the Node
    camNode = new CameraNode("cam", cam);
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

    // Refresh paths at app init — field initializers may have run before Runtime
    // config was applied (Eclipse often uses src/main/resources/resource).
    refreshAssetPaths();

    assetManager.registerLocator("./", FileLocator.class);
    assetManager.registerLocator(getDataDir(), FileLocator.class);
    assetManager.registerLocator(assetsDir, FileLocator.class);
    assetManager.registerLocator(modelsDir, FileLocator.class);
    assetManager.registerLocator(getResourceDir(), FileLocator.class);
    // Also register fallback model dirs (extracted /resource with VinMoov5.j3o)
    for (String modelPath : getModelsSearchPaths()) {
      assetManager.registerLocator(modelPath, FileLocator.class);
      File parentAssets = new File(modelPath).getParentFile();
      if (parentAssets != null) {
        assetManager.registerLocator(parentAssets.getPath(), FileLocator.class);
      }
      log.info("Registered JME model locator {}", modelPath);
    }
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

    // Left drag     orbit around selection / look-at
    // Right drag    pan
    // Shift+drag    pan
    // Wheel         zoom toward look-at
    // Left click    select (if the pointer did not drag); OAK-D mesh → FABRIK
    // https://www.youtube.com/watch?v=IVZPm9HAMD4&feature=youtu.be
    // wrap text of breadcrumbs
    // draggable - resize for menu - what you set is how it stays
    // when menu active - inputs(hotkey when non-menu) should be deactive

    inputManager.addMapping("mouse-click-left", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(this, "mouse-click-left");

    inputManager.addMapping("mouse-click-right", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(this, "mouse-click-right");
    
    inputManager.addMapping("mouse-click-middle", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
    inputManager.addListener(this, "mouse-click-middle");

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

    // Absolute cursor motion — MouseAxisTrigger X/Y is silent with a visible cursor.
    inputManager.addRawInputListener(new RawInputListener() {
      @Override
      public void beginInput() {
      }

      @Override
      public void endInput() {
      }

      @Override
      public void onJoyAxisEvent(JoyAxisEvent evt) {
      }

      @Override
      public void onJoyButtonEvent(JoyButtonEvent evt) {
      }

      @Override
      public void onKeyEvent(KeyInputEvent evt) {
      }

      @Override
      public void onMouseButtonEvent(MouseButtonEvent evt) {
        boolean pressed = evt.isPressed();
        int button = evt.getButtonIndex();
        if (button == MouseInput.BUTTON_LEFT) {
          mouseLeft = pressed;
          if (pressed) {
            viewDragging = false;
            viewDragAccum = 0f;
            captureCursor();
          }
        } else if (button == MouseInput.BUTTON_MIDDLE) {
          mouseMiddle = pressed;
          if (pressed) {
            captureCursor();
          }
        } else if (button == MouseInput.BUTTON_RIGHT) {
          mouseRightPressed = pressed;
          if (pressed) {
            captureCursor();
          }
        }
      }

      @Override
      public void onMouseMotionEvent(MouseMotionEvent evt) {
        updateViewDrag();
      }

      @Override
      public void onTouchEvent(TouchEvent evt) {
      }
    });

    viewPort.setBackgroundColor(ColorRGBA.Gray);

    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));

    rootNode.addLight(sun);

    // AmbientLight sun = new AmbientLight();
    rootNode.addLight(sun);
    // rootNode.scale(.5f);
    // rootNode.setLocalTranslation(0, -200, 0);
    rootNode.setLocalTranslation(0, 0, 0);

    if (usePhysics) {
      bulletAppState.setDebugEnabled(false);
      PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
      PhysicsTestHelper.createBallShooter(app, rootNode, bulletAppState.getPhysicsSpace());
    }

  }

  public void simpleUpdate(float tpf) {

    // start the clock on how much time we will take
    startUpdateTs = System.currentTimeMillis();

    updateHandPositionHud();

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

    // After queued lookAt/move so the user's drag wins this frame.
    updateViewDrag();

    deltaMs = System.currentTimeMillis() - startUpdateTs;
    sleepMs = 33 - deltaMs;

    if (sleepMs < 0) {
      sleepMs = 0;
    }
    // Don't cap the frame rate while dragging — cursor sampling needs to keep up.
    if (!(mouseLeft || mouseMiddle || mouseRightPressed)) {
      sleep(sleepMs);
    }
  }

  public SimpleApplication start() {
    return start(defaultAppType, defaultAppType);
  }

  // dynamic create of type... TODO fix name start --> create
  synchronized public SimpleApplication start(String appName, String appType) {
    if (Service.isHeadless()) {
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
      settings.setResizable(true);
      app.setSettings(settings);

      app.setShowSettings(false); // resolution bps etc dialog
      app.setPauseOnLostFocus(false);

      // the all important "start" - anyone goofing around with the engine
      // before this is done will
      // will generate error from jmonkey - this should "block"
      mainThread = new Thread() {
        @Override
        public void run() {
          try {
            app.start();
          } catch (Throwable t) {
            log.error("JMonkeyEngine app.start() failed", t);
          }
        }
      };
      mainThread.setName(String.format("%s-jme", getName()));
      mainThread.setDaemon(false);
      mainThread.start();

      Callable<String> callable = new Callable<String>() {
        @Override
        public String call() throws Exception {
          log.info("JMonkeyEngine app initialized");
          return "Callable Result";
        }
      };
      Future<String> future = app.enqueue(callable);
      try {
        // Timeout so a failed LWJGL/native init cannot hang Runtime forever
        future.get(60, java.util.concurrent.TimeUnit.SECONDS);

        // default positioning
        moveTo(CAMERA, 0, 3, 6);
        cameraLookAtRoot();
        rotateOnAxis(CAMERA, "x", -20);
        setFloorGrid(true);

      } catch (java.util.concurrent.TimeoutException e) {
        error("JMonkeyEngine failed to initialize within 60s — check LWJGL natives / display");
        log.error("JMonkeyEngine init timeout", e);
      } catch (Exception e) {
        log.warn("future threw", e);
      }
      return app;
    }
    info("already started app %s", appType);

    return app;
  }

  @Override
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

      // WARNING - WE CANNOT PROCESS CONFIG UNTIL THE JMONKYENGINE IS STARTED
      loadDelayed((JMonkeyEngineConfig) config);

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
        // sleep()
        // mainThread.interrupt(); // bigger hammer
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
    setVisible(CullHint.Always == selectedForView.getCullHint());
  }

  public String toJson(Node node) {
    // get absolute position info
    return CodecUtils.toJson(node);
    // save it out
  }

  public static void main(String[] args) {
    try {

      // FIXME - fix menu input system - use jme.rotate/rotateTo/move/moveTo
      // etc.
      // FIXME - node/userdata can have a Map<String, String> of
      // reservedRotations from different controllers
      // FIXME - make "load" work ..


      LoggingFactory.init("WARN");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      
      
      boolean done = true;
      if (done) {
        return;
      }

      Runtime.start("sim", "JMonkeyEngine");

      boolean worky = false;
      if (worky) {
        Runtime.setConfig("dewey-2");
        InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");
        i01.startPeer("simulator");

      } else {
        Runtime.setConfig("dewey-3");
        InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");
        i01.startPeer("simulator");
      }

      Runtime.getInstance().setVirtual(true);
      // Runtime.main(new String[] { "--interactive", "--id", "admin" });
      JMonkeyEngine jme = (JMonkeyEngine) Runtime.start("simulator", "JMonkeyEngine");

      jme.addBox("box", 1.0, 1.0, 1.0, "fc8803", true);
      jme.getNode(CAMERA).move(3, 1, 4);
      jme.cameraLookAt("box");
      jme.setFloorGrid(true);
      // jme.rotateOnAxis("camera", "y", 220.0, 1);
      Node m = jme.getNode("Gui Node");
      // jme.getNode("Gui Node").move(1,1,1);
      jme.getMenuNode().move(1.0f, 3.0f, 2.0f);

      // Runtime.start("gui", "SwingGui");

      // Arduino left = (Arduino) Runtime.start("i01.left", "Arduino");
      // left.connect("COM4");

      Runtime.start("i01.head.jaw", "Servo");

      jme.setRotation("i01.head.jaw", "x");

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

  /**
   * "Near" instantaneous movement - this would be pulsed many times by an
   * actual encoder - represeting its position "right now" vs something else
   * sending a "batch" of changes based on a target position and speed
   */
  @Override
  public void onEncoderData(EncoderData data) {
    String name = data.source;

    String[] multi = multiMapped.get(name);
    if (multi != null) {
      for (String nodeName : multi) {
        // wrong - uses interpolator - which does encoding/animation
        // rotateOnAxis(nodeName, null, data.angle/*, velocity - No speed
        // supplied*/);
        // util.rotateTo(nodeName, null, data.angle);
        addMsg("rotateTo", nodeName, null, data.angle);
      }
    } else {
      // wrong - uses interpolator - which does encoding/animation
      // rotateOnAxis(name, null, data.angle/*, velocity - No speed supplied*/);
      // util.rotateTo(name, null, data.angle);
      addMsg("rotateTo", name, null, data.angle);
    }
  }

  /**
   * parameter is not an interface to allow it to be remotely invoked with the
   * MethodCache
   * 
   * FIXME REMOVE !!!
   * 
   */
  @Override
  public void onServoMoveTo(ServoControl servo) {
    if (servo == null) {
      return;
    }
    Double velocity = servo.getSpeed();
    if (velocity == null || velocity == -1) {
      velocity = defaultServoSpeed;
    }
    rotateNamedNode(servo.getName(), servo.getTargetPos(), velocity);
  }

  /**
   * Callback for {@code Servo.publishServoMoveTo(ServoMove)} — the path used by
   * {@link Servo#processMove}. Applies target input angle immediately so the
   * simulator moves even if TimeEncoder is delayed/disabled.
   */
  public void onServoMove(ServoMove move) {
    if (move == null || move.name == null || move.inputPos == null) {
      return;
    }
    rotateNamedNode(move.name, move.inputPos, defaultServoSpeed);
  }

  private void rotateNamedNode(String name, double degrees, Double velocity) {
    String[] multi = multiMapped.get(name);
    if (multi != null) {
      for (String nodeName : multi) {
        if (velocity != null) {
          rotateOnAxis(nodeName, null, degrees, velocity);
        } else {
          addMsg("rotateTo", nodeName, null, degrees);
        }
      }
    } else if (velocity != null) {
      rotateOnAxis(name, null, degrees, velocity);
    } else {
      addMsg("rotateTo", name, null, degrees);
    }
  }

  public UserDataConfig toUserDataConfig(UserData userData) {
    UserDataConfig udc = new UserDataConfig(userData.mapper, userData.rotationMask);
    return udc;
  }

  @Override
  public JMonkeyEngineConfig getConfig() {
    super.getConfig();

    if (config.models != null) {
      Collections.sort(config.models);
    }

    // WARNING - getConfig is "used" before the delayed apply is processed
    // so if you detroy things here - ie clear nodes, you will be unable to load
    // them appropriately
    // you need to guard with null checking
    for (String key : nodes.keySet()) {
      config.nodes.put(key, toUserDataConfig(nodes.get(key)));
    }

    if (multiMapped != null && multiMapped.size() > 0) {
      // FIXME - FIXED ! config.multiMapped = multiMapped; <- MUST DO NON
      // DESTRUCTIVE ADDITION
      config.multiMapped.putAll(multiMapped);
    }

    // generate defaults end ---------
    return config;
  }
  
  /**
   * Scans and loads the default resource location and loads any models not already loaded
   */
  public void loadDefaultModels() {
    refreshAssetPaths();
    // Load each model basename at most once across all search directories
    LinkedHashSet<String> pendingBasenames = new LinkedHashSet<>();
    for (String dir : getModelsSearchPaths()) {
      log.info("Scanning for JME models in {}", dir);
      for (String name : scanForModels(dir)) {
        String base = modelBasename(name);
        if (!pendingBasenames.add(base)) {
          log.info("Skipping duplicate model file {} also found in {}", base, dir);
        }
      }
    }

    int loaded = 0;
    for (String base : pendingBasenames) {
      Spatial spatial = loadModel(base);
      if (spatial != null) {
        loaded++;
      }
    }
    removeDuplicateVinMoovRoots("i01");
    if (loaded == 0 && !hasVinMoovOrRobotRoot()) {
      error("No JME models loaded — place VinMoov5.j3o under resource/JMonkeyEngine/assets/Models/");
    } else {
      bindVinMoovRoot("i01");
      removeDuplicateVinMoovRoots("i01");
    }
  }

  /**
   * Recompute assets/models dirs from the current resource root.
   */
  public void refreshAssetPaths() {
    assetsDir = getResourceDir() + File.separator + "assets";
    modelsDir = assetsDir + File.separator + "Models";
  }

  /**
   * Candidate directories that may contain VinMoov / other models. Dev Eclipse
   * configs often point resource at {@code src/main/resources/resource} (no large
   * j3o), while the extracted model lives under {@code resource/...}.
   */
  public List<String> getModelsSearchPaths() {
    List<String> paths = new ArrayList<>();
    LinkedHashSet<String> unique = new LinkedHashSet<>();

    refreshAssetPaths();
    unique.add(modelsDir);
    unique.add(FileIO.gluePaths("resource", "JMonkeyEngine/assets/Models"));
    unique.add(FileIO.gluePaths("target/myrobotlab-0.0.1-SNAPSHOT/resource", "JMonkeyEngine/assets/Models"));

    for (String p : unique) {
      File dir = new File(p);
      if (dir.isDirectory()) {
        paths.add(dir.getPath());
      }
    }
    return paths;
  }

  /**
   * Rename VinMoov* root spatial to the InMoov service name so peer node keys
   * like {@code i01.leftArm.bicep} resolve.
   */
  public void bindVinMoovRoot(String robotName) {
    if (robotName == null) {
      return;
    }
    removeDuplicateVinMoovRoots(robotName);
    if (get(robotName) != null) {
      log.info("Scene already has root node {}", robotName);
      return;
    }
    for (String candidate : new String[] { "VinMoov5", "VinMoov4", "VinMoov", "VinMoov5.j3o" }) {
      Spatial spatial = get(candidate);
      if (spatial != null) {
        spatial.setName(robotName);
        log.info("Bound model root {} -> {}", candidate, robotName);
        return;
      }
    }
    log.warn("Could not find VinMoov root to rename to {} — check loaded model node names", robotName);
  }
  
  /**
   * Scans and loads all files from a modelPath directory
   * @param modelPath
   */
  public void loadModels(String modelPath) {
    List<String> models = scanForModels(modelPath);
    for (String path : models) {
      loadModel(path);
    }
  }

  public ServiceConfig loadDelayed(ServiceConfig c) {
    JMonkeyEngineConfig config = (JMonkeyEngineConfig) c;

    if (config.models != null && config.models.size() > 0) {
      List<String> tempList = new ArrayList<>(config.models);
      for (String modelPath : tempList) {
        loadModel(modelPath);
      }
    } else {
      // scan resource dir
      loadDefaultModels();
    }

    // Bind + mappers must run on the JME render thread. Doing this from the
    // service/main thread after app.start() can deadlock and hang startService
    // (demo never reaches ik3d / motion loops).
    final String robotName = inferRobotNameFromNodeConfig(config);
    final String lookAt = config.cameraLookAt;
    Runnable sceneSetup = () -> {
      if (robotName != null) {
        bindVinMoovRoot(robotName);
      }
      applyNodeMappings(config);
      if (lookAt != null) {
        cameraLookAt(lookAt);
      }
      ensureChestDepthCameraOnRenderThread(config);
    };

    if (app != null) {
      try {
        app.enqueue(() -> {
          sceneSetup.run();
          return null;
        }).get(30, java.util.concurrent.TimeUnit.SECONDS);
      } catch (Exception e) {
        log.error("loadDelayed scene setup failed or timed out — continuing without full node mappers", e);
      }
    } else {
      sceneSetup.run();
    }

    invoke("publishSceneReady", getName());
    return c;
  }

  /**
   * Re-apply rotation axes / mappers from config after the model root is bound.
   * Safe to call multiple times (e.g. after {@link #bindVinMoovRoot(String)}).
   */
  public void applyNodeMappings() {
    applyNodeMappings((JMonkeyEngineConfig) config);
  }

  public void applyNodeMappings(JMonkeyEngineConfig config) {
    if (config == null) {
      return;
    }
    int applied = 0;
    int missing = 0;
    if (config.nodes != null) {
      for (String path : config.nodes.keySet()) {
        UserData ud = getUserData(path);
        UserDataConfig udc = config.nodes.get(path);

        if (ud == null) {
          log.debug("could not find node for {}", path);
          missing++;
          continue;
        }

        if (udc.mapper != null) {
          MapperLinear m = udc.mapper;
          setMapper(path, m.minX, m.maxX, m.minY, m.maxY);
        }
        if (udc.rotationMask != null) {
          setRotation(path, udc.rotationMask);
        }
        // remember the authored pose now, before anything drives the joint, so
        // joint angles are always measured from bind
        ud.captureBindRotation();
        applied++;
      }
    }

    if (config.multiMapped != null) {
      for (String name : config.multiMapped.keySet()) {
        multiMap(name, config.multiMapped.get(name));
      }
    }
    log.info("Applied {} node mappings ({} missing)", applied, missing);
  }

  private static String inferRobotNameFromNodeConfig(JMonkeyEngineConfig config) {
    if (config == null || config.nodes == null || config.nodes.isEmpty()) {
      return null;
    }
    for (String path : config.nodes.keySet()) {
      if (path == null) {
        continue;
      }
      int dot = path.indexOf('.');
      if (dot > 0) {
        return path.substring(0, dot);
      }
    }
    return null;
  }

  /**
   * Returns spatial names in the scene that contain the given substring (for
   * diagnostics when servo↔node wiring fails).
   */
  public List<String> findSpatialNamesContaining(String fragment) {
    List<String> matches = new ArrayList<>();
    if (rootNode == null || fragment == null) {
      return matches;
    }
    collectSpatialNamesContaining(rootNode, fragment, matches);
    return matches;
  }

  private void collectSpatialNamesContaining(Spatial spatial, String fragment, List<String> matches) {
    if (spatial == null) {
      return;
    }
    String n = spatial.getName();
    if (n != null && n.contains(fragment)) {
      matches.add(n);
    }
    if (spatial instanceof Node) {
      for (Spatial child : ((Node) spatial).getChildren()) {
        collectSpatialNamesContaining(child, fragment, matches);
      }
    }
  }

  /**
   * Scan a directory for models, perhaps filtering should be done,
   * but I don't know all the possible 3d model files JMonkeyEngine is 
   * currently capable of rendering and don't want to prematurely limit
   * it.
   * @param modelDir
   * @return
   */
  public List<String> scanForModels(String modelDir) {
    List<String> models = new ArrayList<>();
    
    if (modelDir == null) {
      error("models directory cannot be null");
      return models;
    }
    
    File dir = new File(modelDir);
    if (!dir.exists() || !dir.isDirectory()) {
      error("%s models directory is not valid");
      return models;
    }
   
    for(File file : dir.listFiles()) {
      
//      Path pathAbsolute = Paths.get(file.getAbsolutePath());
//      Path pathBase = Paths.get(System.getProperty("user.dir"));
//      Path pathRelative = pathBase.relativize(pathAbsolute);      
//      models.add(pathRelative.toString());
      models.add(file.getName());
    }
    
    return models;
  }

  // @Override
  // public ServiceConfig apply(ServiceConfig c) {
  // JMonkeyEngineConfig config = (JMonkeyEngineConfig) super.apply(c);
  // if (app != null) {
  // // if there is an app we can load immediately
  // loadDelayed(config);
  // }
  // return config;
  // }

  public void multiMap(String name, String... nodeNames) {
    if (nodeNames != null) {
      multiMapped.put(name, nodeNames);
    }
  }

  @Override
  public void onServoStarted(String name) {
    log.info("Jme On Servo Started {}", name);
  }

  @Override
  public void onServoStopped(String name) {
    log.info("Jme On Servo Stopped {}", name);
  }

  @Override
  public void onServoStop(ServoControl sc) {
    // TODO Auto-generated method stub
    log.info("Jme On Servo Stop with the servo control {}", sc);
  }

  @Override
  public void onServoDisable(ServoControl sc) {
    // TODO Auto-generated method stub
    log.info("Jme onServoDisable with the servo control {}", sc);
  }

  @Override
  public void onServoEnable(ServoControl sc) {
    log.info("Jme onServoEnable SC {}", sc);
  }

  @Override
  public void onServoEnable(String name) {
    log.info("Jme onServoEnable {}", name);
  }

  @Override
  public void onMoveTo(ServoControl sc) {
    // TODO Auto-generated method stub
    log.info("Jme onMoveTo SC {}", sc);
  }

  @Override
  public void onServoSetSpeed(ServoControl sc) {
    // TODO Auto-generated method stub
    log.info("Jme onServoSetSpeed SC {}", sc);
  }

}