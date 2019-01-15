package org.myrobotlab.service;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.FloatBuffer;
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
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.jme3.AnalogHandler;
import org.myrobotlab.jme3.HudText;
import org.myrobotlab.jme3.Jme3App;
import org.myrobotlab.jme3.Jme3ServoController;
import org.myrobotlab.jme3.Jme3Util;
import org.myrobotlab.jme3.MainMenuState;
import org.myrobotlab.jme3.PhysicsTestHelper;
import org.myrobotlab.jme3.UserData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.abstracts.AbstractComputerVision;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.virtual.VirtualMotor;
import org.slf4j.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bullet.BulletAppState;
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
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.plugins.blender.BlenderLoader;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;

/**
 * A simulator built on JMonkey 3 Engine.
 * 
 * @author GroG, calamity, kwatters, moz4r and many others ...
 *
 */
public class JMonkeyEngine extends Service implements ActionListener,
    SceneGraphVisitor /* , AnalogListener - can't do both jmonkey bug */ {

  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);
  private static final long serialVersionUID = 1L;

  transient Jme3ServoController servoController;
  
  transient AppStateManager stateManager;

  final Map<String, String> nameMappings = new TreeMap<String, String>();
  
  transient BulletAppState bulletAppState;

  final public String KEY_SEPERATOR = "/";

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
    String jmeVersion = "3.2.2-stable";
    meta.addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);
    // meta.addDependency("org.jmonkeyengine", "jme3-test-data", jmeVersion);
    // meta.addDependency("org.jmonkeyengine", "jme3-niftygui", jmeVersion); Yay
    // ! bye bye
    meta.addDependency("com.simsilica", "lemur", "1.11.0");
    meta.addDependency("com.simsilica", "lemur-proto", "1.10.0");

    // meta.addDependency("org.jmonkeyengine", "jme3-bullet", jmeVersion);
    // meta.addDependency("org.jmonkeyengine", "jme3-bullet-native",
    // jmeVersion);

    meta.addDependency("jme3utilities", "Minie", "0.6.2");

    // "new" physics - ik forward kinematics ...

    // not really supposed to use blender models - export to j3o
    meta.addDependency("org.jmonkeyengine", "jme3-blender", jmeVersion);

    // jbullet ==> org="net.sf.sociaal" name="jME3-jbullet" rev="3.0.0.20130526"

    // audio dependencies
    meta.addDependency("de.jarnbjo", "j-ogg-all", "1.0.0");

    meta.addCategory("simulator");
    return meta;
  }

  public static void main(String[] args) {
    try {

      // FIXME - ADD DEFAULT ROTATION !!! ... default"move" & default"rotate" &
      // default rotationalMask !!!
      // FIXME - something to autoAttach ! ... even at origin position

      LoggingFactory.init("info");

      Runtime.start("gui", "SwingGui");
      // Runtime.start("gui", "SwingGui");
      JMonkeyEngine jme = (JMonkeyEngine) Runtime.start("jme", "JMonkeyEngine");
      jme.addGrid();
      
      /*
      jme.rename("head-6", "i01.head.rotHead");
      jme.setMapper("i01.head.rotHead", 0, 180, -90, 90); // shift 90 degrees
      */
      jme.addBox("floor.box.01", 1.0f, 1.0f, 1.0f, "003300", true);
      jme.move("floor.box.01", 2, 0);

      jme.addBox("floor.box.01", 1.0f, 1.0f, 1.0f, "003300", true);
      jme.move("floor.box.01", 2, 0);

      /*
      jme.rename("head-6", "i01.head.rotHead");
      jme.setMapper("i01.head.rotHead", 0, 180, -90, 90);
      */

      Servo servo = (Servo) Runtime.start("i01.head.jaw", "Servo");
      jme.setRotation("i01.head.jaw", "x");

      boolean test = true;
      if (test) {
        return;
      }

      // jme.scaleModels("rescaled");

      // FIXME - add christmas hat to inmoov - new years hangover...
      // FIXME -
      // jme.addBox("floor.cube");
      // jme.addBox("floor.tile", 1.01f, 0.001f, 1.0f, "FF0000", true);
      // jme.addBox("ruler", 0.01f, 0.7747f, 0.01f, "FF0000", true); //
      // jme.moveTo("ruler", 0.0f, 0.7f, 0.0f); // VS move relative !
      // jme.bind("cube", "Segway");
      // jme.bind("i01.torso.lowStom", "segway");
      // jme.saveNode("i01.torso.lowStom");
      // jme.enableBoundingBox("segway", true);
      // jme.scale("segway", 5.0f);
      // jme.saveToJson("scene.json");
      // jme.scale("i01.torso.lowStom", 1/400f); // FIXME - fix the models scale
      // !!! :P - btw - this might be a single model !!!
      // jme.scale("i01.torso.midStom", 1/400f); // FIXME - fix the models scale
      // !!! :P - btw - this might be a single model !!!
      // jme.scale("i01.torso.topStom", 1/400f); // FIXME - fix the models scale
      // !!! :P - btw - this might be a single model !!!

      jme.setRotation("i01.torso.lowStom", "x");

      jme.setRotation("i01.torso.midStom", "-y");
      // jme.setMapper("i01.torso.midStom", 0, 180, 120, 60);
      jme.rotateOnAxis("i01.torso.midStom", "y", 180);
      jme.bind("i01.torso.midStom", "i01.torso.lowStom");
      // jme.bind("i01.torso.midStom", "root");

      boolean done = true;
      if (done) {
        return;
      }

      jme.setRotation("i01.torso.topStom", "z");
      jme.setMapper("i01.torso.topStom", 0, 180, 80, 100);
      // jme.rotateOnAxis("i01.torso.topStom", "x", 90);
      jme.moveTo("i01.torso.topStom", 0, 0.2625f, 0.025f); // this is a
                                                           // translation to an
                                                           // initial position
      jme.bind("i01.torso.topStom", "i01.torso.midStom");

      jme.putNode("rightS");
      jme.moveTo("rightS", 0, 0.75f, 0);
      jme.setRotation("rightS", "z");
      jme.bind("rightS", "i01.torso.topStom");

      // putNode("i01.rightArm.omoplate", "rightS", "Models/Romoplate1.j3o", new
      // Mapper(0, 180, 10, 70), Vector3f.UNIT_Z.mult(-1), new Vector3f(-143, 0,
      // -17), 0);
      jme.setMapper("i01.rightArm.omoplate", 0, 180, 10, 70);
      jme.moveTo("i01.rightArm.omoplate", -0.3575f, 0, -0.0425f);
      jme.setRotation("i01.rightArm.omoplate", "-z");
      jme.bind("i01.rightArm.omoplate", "rightS");

      // putNode("i01.rightArm.shoulder", "i01.rightArm.omoplate",
      // "Models/Rshoulder1.j3o", new Mapper(0, 180, 0, 180),
      // Vector3f.UNIT_X.mult(-1), new Vector3f(-23, -45, 0), 0);

      // jme.setMapper("i01.rightArm.shoulder", 0, 180, 10, 70);
      jme.moveTo("i01.rightArm.shoulder", -0.0575f, -0.1125f, 0);
      jme.setRotation("i01.rightArm.shoulder", "-x");
      jme.bind("i01.rightArm.shoulder", "i01.rightArm.omoplate");
      // TODO jme.rename()

      ///////////////////////////////// BEGIN
      jme.moveTo("i01.rightArm.rotate", -0.1425f, -0.1375f, 0.02f);
      jme.setRotation("i01.rightArm.rotate", "-y");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 40, 180);
      jme.bind("i01.rightArm.rotate", "i01.rightArm.shoulder");

      jme.moveTo("i01.rightArm.bicep", 0.0125f, -0.5625f, -0.08f);
      jme.setRotation("i01.rightArm.bicep", "-y");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 5, 60);
      jme.bind("i01.rightArm.bicep", "i01.rightArm.rotate");

      jme.putNode("leftS");
      jme.moveTo("leftS", 0, 0.75f, 0);
      jme.setRotation("leftS", "z");
      jme.bind("leftS", "i01.torso.topStom");

      jme.moveTo("i01.leftArm.omoplate", 0.3575f, 0, -0.0375f);
      jme.setRotation("i01.leftArm.omoplate", "z");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 10, 70);
      jme.bind("i01.leftArm.omoplate", "leftS");

      jme.moveTo("i01.leftArm.shoulder", 0.0425f, -0.1125f, 0.0125f);
      jme.setRotation("i01.leftArm.shoulder", "-x");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 10, 70);
      jme.bind("i01.leftArm.shoulder", "i01.leftArm.omoplate");

      jme.moveTo("i01.leftArm.rotate", 0.1625f, -0.145f, -0.0075f);
      jme.setRotation("i01.leftArm.rotate", "-x");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 40, 180);
      jme.bind("i01.leftArm.rotate", "i01.leftArm.shoulder");

      jme.moveTo("i01.leftArm.bicep", -0.035f, -0.5575f, -0.07f);
      jme.setRotation("i01.leftArm.bicep", "-x");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 40, 180);
      jme.bind("i01.leftArm.bicep", "i01.leftArm.rotate");

      jme.moveTo("i01.rightHand.wrist", 0.0375f, -0.725f, -0.025f);
      jme.setRotation("i01.rightHand.wrist", "-x");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 130, 40);
      jme.bind("i01.rightHand.wrist", "i01.rightArm.bicep");

      jme.moveTo("i01.leftHand.wrist", 0, -0.725f, -0.05f);
      jme.setRotation("i01.rightHand.wrist", "y");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 130, 40);
      jme.bind("i01.rightHand.wrist", "i01.leftArm.bicep");
      // jme.rotateOnAxis("i01.leftHand.wrist", 90.0);

      jme.moveTo("i01.head.neck", 0, 1.13125f, -0.1125f);
      jme.setRotation("i01.head.neck", "-x");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 60, 110);
      jme.bind("i01.head.neck", "i01.torso.topStom");

      jme.putNode("i01.head.rollNeck");
      jme.moveTo("i01.head.rollNeck", 0, 0, 0);
      jme.setRotation("i01.head.rollNeck", "z");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 60, 115);
      jme.bind("i01.head.rollNeck", "i01.head.neck");
      // jme.rotateOnAxis("i01.head.rollNeck", 90.0);

      jme.moveTo("i01.head.rothead", 0, 0.025f, 0.05f);
      jme.setRotation("i01.head.rothead", "-y");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 150, 30);
      jme.bind("i01.head.rothead", "i01.head.rollNeck");
      // jme.rotateOnAxis("i01.head.rothead", 90.0);

      jme.moveTo("i01.head.jaw", -0.0125f, 0.15f, -0.125f);
      jme.setRotation("i01.head.jaw", "-x");
      // jme.setMapper("i01.rightArm.rotate", 0, 180, 0, 180);
      jme.bind("i01.head.jaw", "i01.head.rothead");
      // jme.rotateOnAxis("i01.head.rollNeck", 90.0);

      //////////////////////////////// END

      // final scale ..
      // jme.load("c:/mrl/i01.head.rothead.j3o");
      // Jme3Object h = jme.get("i01.head.rothead");

      // jme.scale("i01.head.rothead", 1/400f);

      // h.enableBoundingBox(true);
      // jme.scale("head", 1/400f);

      // final scale ..
      // jme.scale("i01.torso.lowStom", 1/400f);

      // jme.putNode("RightS");

      log.info("done with main");

      // FIXME - move camera jme.move("camera", x, y , z) // root node...

      Geometry geometry = new Geometry();

      Node node = new Node();

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

  public void rotateTo(String name, double degrees) {
    Jme3Msg msg = new Jme3Msg();
    msg.method = "rotateTo";
    msg.data = new Object[] { name, degrees };
    addMsg(msg);
  }

  public void rotateOnAxis(String name, String axis, double degrees) {
    Jme3Msg msg = new Jme3Msg();
    msg.method = "rotateOnAxis";
    msg.data = new Object[] { name, axis, (float) degrees };
    addMsg(msg);
  }

  public void putNode(String name) {
    if (nodes.containsKey(name)) {
      log.error("putNode already has %s", name);
      return;
    }
    Node n = new Node(name);
    // UserData o = new UserData(this, n);
    // putNode(name, null, null, null, null, null, 0);
    rootNode.attachChild(n);
    // nodes.put(name, o);
    index(n);
  }

  public Mapper setMapper(String name, int minx, int maxx, int miny, int maxy) {
    UserData node = getUserData(name);
    if (node == null) {
      error("setMapper %s does not exist", name);
      return null;
    }
    node.mapper = new Mapper(minx, maxx, miny, maxy);
    return node.mapper;
  }

  public class Jme3Msg {
    public String name;
    public String method;
    public Object data[];
  }

  // TODO - need to make thread safe ? JME thread ?
  // turn it into a jme msg - put it on the update queue ?
  public void scale(String name, float scale) {
    UserData node = getUserData(name);
    if (node != null) {
      node.scale(scale);
    } else {
      error("scale %s does not exist", name);
    }
  }

  /**
   * <pre>
   *  FIXME - IMPLEMENT
   public void scaleModels(String dirPath, float scale) {
     File dir = new File(dirPath);
     assetManager.registerLocator(dirPath, FileLocator.class);
     File[] files = dir.listFiles();
     for (File file : files) {
       Jme3Object o = load(file.getAbsolutePath());
       o.getSpatial().scale(scale);
       saveNode(o.getName());
     }
   }
   * </pre>
   */

  transient AnalogListener analog = null;
  // TODO - make intermediate class - which has common interface to grab
  // shapes/boxes
  transient Jme3App app;

  transient MainMenuState menu;
  // real JMonkeyEngine parts ...
  transient AssetManager assetManager;

  boolean autoAttach = true;
  boolean autoAttachAll = true;
  transient Camera camera;
  transient CameraNode camNode;
  // transient Spatial control = null;
  String defaultAppType = "Jme3App";

  long deltaMs;

  DisplayMode displayMode = null;

  // protected Queue<Move> jmeMsgQueue = new ConcurrentLinkedQueue<Move>();
  protected Queue<Jme3Msg> jmeMsgQueue = new ConcurrentLinkedQueue<Jme3Msg>();

  transient FlyByCamera flyCam;

  String fontColor = "#66ff66"; // green

  int fontSize = 14;

  boolean fullscreen = false;

  Map<String, HudText> guiText = new TreeMap<>();

  int height = 768;

  transient InputManager inputManager;

  DisplayMode lastDisplayMode = null;

  transient Map<String, Spatial> nodes = new TreeMap<String, Spatial>();

  // https://stackoverflow.com/questions/16861727/jmonkey-engine-3-0-drawing-points
  FloatBuffer pointCloudBuffer = null;

  Material pointCloudMat = null;

  Mesh pointCloudMesh = new Mesh();

  transient Node rootNode;
  transient Node guiNode;

  transient Spatial selected = null;

  int selectIndex = 0;

  transient AppSettings settings;

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

  long sleepMs;

  long startUpdateTs;

  // transient AnalogListener analogListener;
  transient ViewPort viewPort;

  int width = 1024;

  /**
   * class for doing manipulations of the scene graph with the JME thread do not
   * call its methods directly
   */
  transient Jme3Util util;

  String assetsDir = getDataDir() + File.separator + "assets";
  String modelsDir = assetsDir + File.separator + "Models";

  public JMonkeyEngine(String n) {
    super(n);
    File d = new File(modelsDir);
    d.mkdirs();
    util = new Jme3Util(this);
    analog = new AnalogHandler(this);
    servoController = new Jme3ServoController(this);
  }

  public void addBox(String boxName) {
    addBox(boxName, 1f, 1f, 1f); // room box
  }

  public void addBox(String boxName, float width, float depth, float height) {
    addBox(boxName, width, depth, height, null, null);
    moveTo(boxName, 0f, height, 0f); // center it on the floor fully above the
                                     // ground
  }

  public void addBox(String name, Float width, Float depth, Float height, String color, Boolean fill) {

    // FIXME - auto increment ? addWall ??? addFloor
    if (nodes.containsKey(name)) {
      warn("addBox %s already in nodes", name);
      return;
    }

    if (width == null) {
      width = 1f;
    }

    if (depth == null) {
      depth = 1f;
    }

    if (height == null) {
      height = 1f;
    }

    Box box = new Box(width, depth, height); // 1 meter square

    // wireCube.setMode(Mesh.Mode.LineLoop);
    // box.setMode(Mesh.Mode.Lines);
    // FIXME - geom & matterial always xxx-geometry ? & xxx-material ??
    Geometry geom = new Geometry(name, box);

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
    Node boxNode = new Node(name);

    // FIXME - optimize rootNode/geom/nodes & jme3Node !
    // UserData o = new UserData(this, boxNode);
    // nodes.put(name, o);
    rootNode.attachChild(geom);
    moveTo(name, 0.0f, 0.5f * height, 0.0f);
    index(boxNode);
  }

  public void addGrid() {
    addGrid("floor-grid", new Vector3f(0, 0, 0), 40, "CCCCCC");
  }

  public Geometry addGrid(String name, Vector3f pos, int size, String color) {
    Geometry g = new Geometry("wireframe grid", new Grid(size, size, 1.0f));
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.getAdditionalRenderState().setWireframe(true);
    mat.setColor("Color", Jme3Util.toColor(color));
    g.setMaterial(mat);
    g.center().move(pos);
    Node n = new Node(name);
    n.attachChild(g);
    rootNode.attachChild(n);
    putNode(n);
    return g;
  }

  public void addMsg(Jme3Msg msg) {
    jmeMsgQueue.add(msg);
  }

  public void attach(Attachable service) throws Exception {

    app.attach(service);

    // Cv Publisher ..
    if (service instanceof AbstractComputerVision) {
      AbstractComputerVision cv = (AbstractComputerVision) service;
      subscribe(service.getName(), "publishCvData");
    }

    if (service instanceof ServoControl) {
      servoController.attach(service);
    }

    // backward attach ?
  }

  public VirtualMotor createVirtualMotor(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * cycles through children at same level
   */
  public void cycle() {

    if (selected == null) {
      setSelected(rootNode);
    }

    Node parent = selected.getParent();
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

  public void setSelected(Spatial newSelected) {

    // turn off old
    if (selected != null) {
      enableBoundingBox(selected, false);
      enableCoordinateAxes(selected, false);
    }

    // set selected
    selected = newSelected;

    // display in menu
    menu.putText(newSelected);

    // turn on new
    if (newSelected != null) {
      enableBoundingBox(newSelected, true);
      enableCoordinateAxes(newSelected, true);
    }
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

  public Jme3App getApp() {
    return app;
  }

  public Queue<Jme3Msg> getjmeMsgQueue() {
    return jmeMsgQueue;
  }

  // FIXME - getObject
  /*
   * public UserData getJme3Object(String name) { if (name != null &&
   * nodes.containsKey(name)) { return nodes.get(name); } return null; }
   */

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

      /*
       * if (nodes.containsKey(simpleName)) { warn("already %s in nodes",
       * simpleName); return nodes.get(simpleName); }
       */

      if (!ext.equals("json")) {

        Spatial spatial = assetManager.loadModel(filename);
        spatial.setName(simpleName);

        // we only deal with Nodes !!!
        // if this is a mesh only - it needs a node

        Node node = null;

        if (spatial instanceof Node) {
          node = (Node) spatial;
        } else {
          node = new Node(spatial.getName());
          node.attachChild(spatial);
        }

        // FIXME - putNodes .. travel and add Jme3Object ...
        // use lemur gui.... "loaded xxx " 3 nodes 5 geometries
        // putText(spatial, 10, 10);

        rootNode.attachChild(node);
        index(node);
        // selected = spatial;
        // nodes.put(simpleName, o); // deprecate ...
      } else {

        // now for the json meta data ....

        String json = FileIO.toString(inFileName);
        Map<String, Object> list = CodecUtils.fromJson(json, nodes.getClass());
        for (String name : list.keySet()) {
          String nodePart = CodecUtils.toJson(list.get(name));
          UserData node = CodecUtils.fromJson(nodePart, UserData.class);
          // get/create transient parts
          // node.setService(Runtime.getService(name));
          // node.setJme(this);
          // putN
          // nodes.put(node.getName(), node);
        }
      }

    } catch (Exception e) {
      error(e);
    }
  }

  private String getExt(String name) {
    int pos = name.lastIndexOf(".");
    String ext = null;
    if (pos != -1) {
      ext = name.substring(pos + 1).toLowerCase();
    }
    return ext;
  }

  private String getNameNoExt(String name) {
    int pos = name.lastIndexOf(".");
    String nameNoExt = name;
    if (pos != -1) {
      nameNoExt = name.substring(0, pos);
    }
    return nameNoExt;
  }

  // FIXME - get then write directly to Mesh.getBuffer()
  // then set with mesh.setData(buffer)
  // https://hub.jmonkeyengine.org/t/updating-mesh-vertices/25088/7

  public void loadModels() {
    // load the root data dir
    loadModels(modelsDir);
  }

  public void loadModels(String dirPath) {
    File dir = new File(dirPath);
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
      if (!f.isDirectory() && !"json".equals(getExt(f.getName()))) {
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

  // FIXME - remove - just load a json file
  // NOT TO BE CALLED BY ANY OTHER THREAD BESIDES JME THREAD !!! OR YOU GET A
  // SPATIAL EXCEPTION !
  private void loadInMoov() {

    /**
     * <pre>
     * 
     * [ { "name":"i01.torso.lowStom", "action":"loadModel",
     * "args":["Models/mtorso.j3o"]}, { "name":"i01.torso.lowStom",
     * "action":"setMapper", "args":[0, 180, 120, 60]}, {
     * "name":"i01.torso.lowStom", "action":"setRotation", "args":["x"]}, {
     * "name":"i01.torso.lowStom", "action":"rotate", "args":["x"]}, {
     * "name":"leftS", "action":"putNode", "args":["rightS"]}
     * 
     * ]
     */

    /*
     * UserData o = putNode("i01.torso.lowStom", "rootNode",
     * "Models/ltorso.j3o", null, Vector3f.UNIT_X.mult(1), new Vector3f(0, 0,
     * 0), 0); rootNode.attachChild(o.getNode());
     * 
     * // FIXME - bind this to process Peers !!!! "default" "Peer" info in 3D
     * form
     * 
     * putNode("i01.torso.midStom", "i01.torso.lowStom", "Models/mtorso.j3o",
     * new Mapper(0, 180, 120, 60), Vector3f.UNIT_Y.mult(-1), new Vector3f(0, 0,
     * 0), 0); putNode("i01.torso.topStom", "i01.torso.midStom",
     * "Models/ttorso1.j3o", new Mapper(0, 180, 80, 100),
     * Vector3f.UNIT_Z.mult(1), new Vector3f(0, 105, 10), 0); putNode("rightS",
     * "i01.torso.topStom", null, null, Vector3f.UNIT_Z.mult(1), new Vector3f(0,
     * 300, 0), 0);
     * 
     * // Vector3f angle = rotationMask.mult((float) Math.toRadians(6));
     * putNode("i01.rightArm.omoplate", "rightS", "Models/Romoplate1.j3o", new
     * Mapper(0, 180, 10, 70), Vector3f.UNIT_Z.mult(-1), new Vector3f(-143, 0,
     * -17), 0);
     * 
     * // angle = rotationMask.mult((float) Math.toRadians(-2)); //
     * node.rotate(angle.x, angle.y, angle.z); <------------ additional rotation
     * // ... putNode("i01.rightArm.shoulder", "i01.rightArm.omoplate",
     * "Models/Rshoulder1.j3o", new Mapper(0, 180, 0, 180),
     * Vector3f.UNIT_X.mult(-1), new Vector3f(-23, -45, 0), 0);
     * putNode("i01.rightArm.rotate", "i01.rightArm.shoulder",
     * "Models/rotate1.j3o", new Mapper(0, 180, 40, 180),
     * Vector3f.UNIT_Y.mult(-1), new Vector3f(-57, -55, 8), 0);
     * 
     * // angle = rotationMask.mult((float) Math.toRadians(30)); // additional
     * // rotate ! // node.rotate(angle.x, angle.y, angle.z);
     * putNode("i01.rightArm.bicep", "i01.rightArm.rotate",
     * "Models/Rbicep1.j3o", new Mapper(0, 180, 5, 60),
     * Vector3f.UNIT_X.mult(-1), new Vector3f(5, -225, -32), 0);
     * putNode("leftS", "i01.torso.topStom", "Models/Lomoplate1.j3o", null,
     * Vector3f.UNIT_Z.mult(1), new Vector3f(0, 300, 0), 0);
     * 
     * // angle = rotationMask.mult((float) Math.toRadians(4)); //
     * node.rotate(angle.x, angle.y, angle.z); <-- another rotation ...
     * putNode("i01.leftArm.omoplate", "leftS", "Models/Lomoplate1.j3o", new
     * Mapper(0, 180, 10, 70), Vector3f.UNIT_Z.mult(1), new Vector3f(143, 0,
     * -15), 0); putNode("i01.leftArm.shoulder", "i01.leftArm.omoplate",
     * "Models/Lshoulder.j3o", new Mapper(0, 180, 0, 180),
     * Vector3f.UNIT_X.mult(-1), new Vector3f(17, -45, 5), 0);
     * putNode("i01.leftArm.rotate", "i01.leftArm.shoulder",
     * "Models/rotate1.j3o", new Mapper(0, 180, 40, 180),
     * Vector3f.UNIT_Y.mult(1), new Vector3f(65, -58, -3), 0);
     * 
     * // angle = rotationMask.mult((float) Math.toRadians(27)); //
     * node.rotate(angle.x, angle.y, angle.z); <-------------- additional rotate
     * // !!! putNode("i01.leftArm.bicep", "i01.leftArm.rotate",
     * "Models/Lbicep.j3o", new Mapper(0, 180, 5, 60), Vector3f.UNIT_X.mult(-1),
     * new Vector3f(-14, -223, -28), 0);
     * 
     * // angle = rotationMask.mult((float) Math.toRadians(-90)); <- Ha .. and
     * an // additional rotate // node.rotate(angle.x, angle.y, angle.z);
     * putNode("i01.rightHand.wrist", "i01.rightArm.bicep",
     * "Models/RWristFinger.j3o", new Mapper(0, 180, 130, 40),
     * Vector3f.UNIT_X.mult(-1), new Vector3f(15, -290, -10), 0);
     * 
     * // angle = rotationMask.mult((float) Math.toRadians(-90)); <--- HA .. and
     * // additional rotation ! // node.rotate(angle.x, angle.y, angle.z);
     * putNode("i01.leftHand.wrist", "i01.leftArm.bicep",
     * "Models/LWristFinger.j3o", new Mapper(0, 180, 40, 130),
     * Vector3f.UNIT_Y.mult(1), new Vector3f(0, -290, -20), 90);
     * putNode("i01.head.neck", "i01.torso.topStom", "Models/neck.j3o", new
     * Mapper(0, 180, 60, 110), Vector3f.UNIT_X.mult(-1), new Vector3f(0,
     * 452.5f, -45), 0); putNode("i01.head.rollNeck", "i01.head.neck", null, new
     * Mapper(0, 180, 60, 115), Vector3f.UNIT_Z.mult(1), new Vector3f(0, 0, 0),
     * 90); putNode("i01.head.rothead", "i01.head.rollNeck", "Models/head.j3o",
     * new Mapper(0, 180, 150, 30), Vector3f.UNIT_Y.mult(-1), new Vector3f(0,
     * 10, 20), 90); putNode("i01.head.jaw", "i01.head.rothead",
     * "Models/jaw.j3o", new Mapper(0, 180, 0, 180), Vector3f.UNIT_X.mult(-1),
     * new Vector3f(-5, 60, -50), 90);
     * 
     * saveToJson("inmoov-jme.json");
     * 
     * // Vector3D vector = new Vector3D(7, 3, 120); load("inmoov-jme.json");
     * 
     * saveToJson("inmoov-jme1.json"); o.getNode().scale(1 / 400f);
     */
  }

  public Spatial loadModel(String assetPath) {
    return assetManager.loadModel(assetPath);
  }

  // relative move
  // favor the xy plane because we are not birds ?
  public void move(String name, float x, float y) {
    // must "get z"
    // move(name, x, y, null);
  }

  public void moveTo(String name, float x, float y, float z) {
    Jme3Msg msg = new Jme3Msg();
    msg.method = "moveTo";
    msg.data = new Object[] { name, x, y, z };
    addMsg(msg);
  }

  boolean shiftLeftPressed = false;
  boolean altLeftPressed = false;
  boolean ctrlLeftPressed = false;

  @Override
  public void onAction(String name, boolean keyPressed, float tpf) {
    log.info("onAction {} {} {}", name, keyPressed, tpf);
    if ("full-screen".equals(name)) {
      enableFullScreen(true);
    } else if ("menu".equals(name)) {
      menu.setEnabled(true);
    } else if ("select-root".equals(name)) {
      setSelected(rootNode);
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
      saveNode(selected.getName());
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
    log.info("onAnalog {} {} {}", name, keyPressed, tpf);
    
    if (selected == null) {
      return;
    }

    if (name.equals("mouse-click-left")) {
      // alt + ctrl + lmb = zoom in / zoom out
      // alt + lmb = orbit
      // shift + alt + lmb = pan

      // arrow up/down/left/right = pan
      // shift + up/down = zoom in / zoom out

      if (altLeftPressed && ctrlLeftPressed) {
        log.info("here");
        selected.move(0, 0, keyPressed * -1);
      } else {
        selected.rotate(0, -keyPressed, 0);
      }

      // else SELECT !!!

    } else if (name.equals("mouse-click-right")) {
      // rotate+= keyPressed;
      selected.rotate(0, keyPressed, 0);
      // log.info(rotate);
    } else if (name.equals("mouse-wheel-up") || name.equals("forward")) {
      // selected.setLocalScale(selected.getLocalScale().mult(1.0f));
      selected.move(0, 0, keyPressed * -1);
    } else if (name.equals("mouse-where-down") || name.equals("backward")) {
      // selected.setLocalScale(selected.getLocalScale().mult(1.0f));
      selected.move(0, 0, keyPressed * 1);
    } else if (name.equals("up")) {
      if (ctrlLeftPressed) {
        selected.move(0, 0, keyPressed * 1);
      } else {
        selected.move(0, keyPressed * 1, 0);
      }
    } else if (name.equals("down")) {

      if (ctrlLeftPressed) {
        selected.move(0, 0, keyPressed * -1);
      } else {
        selected.move(0, -keyPressed * 1, 0);
      }
    } else if (name.equals("left")) {
      if (ctrlLeftPressed) {
        selected.rotate(0, -keyPressed, 0);
      } else {
        selected.move(-keyPressed * 1, 0, 0);
      }
    } else if (name.equals("right")) {
      if (ctrlLeftPressed) {
        selected.rotate(0, keyPressed, 0);
      } else {
        selected.move(keyPressed * 1, 0, 0);
      }
    }

    if (name.equals("pick-target")) {
      // Reset results list.
      CollisionResults results = new CollisionResults();
      // Convert screen click to 3d position
      Vector2f click2d = inputManager.getCursorPosition();
      Vector3f click3d = camera.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
      Vector3f dir = camera.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
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
        setSelected(target);
      }
    } // else if ...
    /*
     * if (selected != null) { putText(selected, 10, 10);
     * menu.putText(selected); }
     */

    menu.putText(selected);
  }

  /**
   * A method to accept Computer Vision data (from OpenCV or BoofCv) and to
   * appropriately delegate it out to more specific methods
   * 
   * @param data
   */
  public void onCvData(CvData data) {
    onPointCloud(data.getPointCloud());
  }

  public void onPointCloud(PointCloud pc) {

    if (pc == null) {
      return;
    }
    // pointCloudMat.setBoolean("VertexColor", false);
    // pointCloudMesh.setPointSize(0.01f);

    if (pointCloudBuffer == null) {
      initPointCloud(pc);
      addBox("box-1");
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

  public void rotate(String object, String axis, float degrees) {
    // TODO Auto-generated method stub

  }

  // FIXME - rename saveToJson
  // FIXME - base64 encoding of j3o file - "all in one file" gltf instead ???
  public boolean saveToJson(String jsonPath) {
    try {
      String json = CodecUtils.toJson(nodes);
      FileIO.toFile(jsonPath, json.getBytes());
      return true;
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  public String toJson(Node node) {
    // get absolute position info
    return CodecUtils.toJson(node);
    // save it out
  }

  public boolean saveNode(String name) {
    return saveNode(name, null);
  }

  public boolean saveNode(String name, String filename) {
    try {

      if (filename == null) {
        filename = name + ".j3o";
      }

      Spatial spatial = rootNode.getChild(name);
      if (spatial == null) {
        error("could not save {} - node not found", name);
      }

      BinaryExporter exporter = BinaryExporter.getInstance();
      FileOutputStream out = new FileOutputStream(filename);
      Node n = (Node) spatial;
      exporter.save(n.getChild(0), out);
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

  public void setVisible(boolean b) {
    if (selected != null) {
      if (b) {
        selected.setCullHint(Spatial.CullHint.Inherit);
      } else {
        selected.setCullHint(Spatial.CullHint.Always);
      }
    }
  }

  public void simpleInitApp() {

    // GuiGlobals.initialize(app);
    // Load the 'glass' style
    // BaseStyles.loadGlassStyle();
    // Set 'glass' as the default style when not specified
    // GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

    setDisplayFps(false);

    setDisplayStatView(false);

    guiNode = app.getGuiNode();

    // wtf - assetManager == null - another race condition ?!?!?
    // after start - these are initialized as "default"
    assetManager = app.getAssetManager();
    inputManager = app.getInputManager();
    flyCam = app.getFlyByCamera();
    camera = app.getCamera();

    rootNode = app.getRootNode();
    rootNode.setName("root");

    viewPort = app.getViewPort();
    // Setting the direction to Spatial to camera, this means the camera will
    // copy the movements of the Node
    camNode = new CameraNode("cam", camera);
    camNode.setControlDir(ControlDirection.SpatialToCamera);
    // rootNode.attachChild(camNode);
    // camNode.attachChild(child)
    // camera.setLocation(new Vector3f(0, 1, -1));
    camNode.setLocalTranslation(-1, 1, -1);
    // camNode.setLocalTranslation(new Vector3f(1f, 1f, 1f));
    camNode.lookAt(rootNode.getLocalTranslation(), Vector3f.UNIT_Y);
    // rootNode.attachChild(camNode);
    // rootNode.attachChild(cam);

    // personNode.attachChild(camNode);
    // Screen screen = nifty.getCurrentScreen();

    // loadNiftyGui();

    // cam.setFrustum(0, 1000, 0, 0, 0, 0);
    // cam.setFrustumNear(1.0f);

    inputManager.setCursorVisible(true);
    if (flyCam != null) {
      flyCam.setEnabled(false);
    }
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
    assetManager.registerLocator("InMoov/jm3/assets", FileLocator.class);
    assetManager.registerLoader(BlenderLoader.class, "blend");
    stateManager = app.getStateManager();
    
    /**<pre> Physics related
    bulletAppState = new BulletAppState();
    bulletAppState.setEnabled(true);
    stateManager.attach(bulletAppState);
    PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
    bulletAppState.setDebugEnabled(true);
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

    // inputManager.addMapping("mouse-click-left", new
    // MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    // inputManager.addListener(analog, "mouse-click-left");
    inputManager.addMapping("pick-target", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(analog, "pick-target");

    inputManager.addMapping("mouse-click-right", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(analog, "mouse-click-right");
    inputManager.addMapping("mouse-wheel-up", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
    inputManager.addListener(analog, "mouse-wheel-up");
    inputManager.addMapping("mouse-where-down", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
    inputManager.addListener(analog, "mouse-where-down");

    inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
    inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
    inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
    inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
    inputManager.addMapping("forward", new KeyTrigger(KeyInput.KEY_J));
    inputManager.addMapping("backward", new KeyTrigger(KeyInput.KEY_K));

    inputManager.addListener(analog, new String[] { "left", "right", "up", "down", "forward", "backward" });

    inputManager.addMapping("select-root", new KeyTrigger(KeyInput.KEY_R));
    inputManager.addListener(this, "select-root");

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

    // FIXME NOT i01 !! - if a InMoov Type has been identified
    // the InMoov {name} is pulled
    // FIXME - correct names of types :P
    // FIXME - INPUT ALL THIS VIA TEXT/yaml/json config !!!

    putNode(rootNode);

    menu = new MainMenuState(this);
    menu.loadGui();
    // menu.initialize(stateManager, app);

    // AH HAA !!! ... so JME thread can only do this :P
    // loadInMoov();
    loadModels();
  }

  public void putNode(Node node) {
    if (nodes.containsKey(node.getName())) {
      error("%s already loaded", node.getName());
      return;
    }
    index(node);
  }

  public void simpleUpdate(float tpf) {

    // start the clock on how much time we will take
    startUpdateTs = System.currentTimeMillis();

    for (HudText hudTxt : guiText.values()) {
      hudTxt.update();
      // txt.setText("DOOD ITS ALL ABOUT THE THREAD");
    }

    while (jmeMsgQueue.size() > 0) {
      Jme3Msg msg = null;
      try {

        // TODO - support relative & absolute moves
        msg = jmeMsgQueue.remove();
        util.invoke(msg);

        /**
         * <pre>
         * Jme3Object object = getJme3Object(msg.name);
         * 
         * if (object == null) { log.error("no Jme3Object named {}", name); }
         * object.rotateDegrees(move.getAngle());
         */

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
      } catch (Exception e) {
        log.warn("future threw", e);
      }
      return app;
    }
    info("already started app %s", appType);
    return app;
  }

  public void startService() {
    super.startService();
    // start the jmonkey app - if you want a diferent Jme3App
    // config should be set at before this time
    start();
    // notify me if new services are created
    subscribe(Runtime.getRuntimeName(), "registered");

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

  }

  public void stop() {
    if (app != null) {

      // why ?
      app.getRootNode().detachAllChildren();
      app.getGuiNode().detachAllChildren();
      app.stop();
      // app.destroy(); not for "us"
      app = null;
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
    if (Spatial.CullHint.Always == selected.getCullHint()) {
      setVisible(true);
    } else {
      setVisible(false);
    }
  }

  public Node getNode(Spatial spatial) {
    if (spatial instanceof Geometry) {
      return spatial.getParent();
    }
    return (Node) spatial;
  }

  public void enableBoundingBox(Spatial spatial, boolean b) {
    Node node = getNode(spatial);
    UserData data = getUserData(node);
    data.enableBoundingBox(b);
  }

  // FIXME !!!! enableCoodinateAxes - same s bb including parent if geometry
  public void enableCoordinateAxes(Spatial spatial, boolean b) {
    Node node = getNode(spatial);
    UserData data = getUserData(node);
    data.enableCoordinateAxes(b);
  }

  public void enableBoundingBox(String name, boolean b) {
    UserData o = getUserData(name);
    enableBoundingBox(o.getSpatial(), b);
  }

  public AssetManager getAssetManager() {
    return assetManager;
  }

  /**
   * binds two objects together ...
   * 
   * @param child
   * @param parent
   */
  public void bind(String child, String parent) {
    Jme3Msg msg = new Jme3Msg();
    msg.method = "bind";
    msg.data = new Object[] { child, parent };
    addMsg(msg);
  }

  public void clone(String name, String newName) {

  }

  public void index(Node node) {
    index(node, false, false);
  }

  // FIXME - implement
  // with or w/o Geometry
  public void index(Spatial spatial, boolean includeGeometries, boolean useDepth) {
    // FIXME - figure out path - is node connected to root ?
    /**
     * if the spatial is connected to the root - then this reprocesses the whole
     * scene graph which "might" be necessary, if there is a change of branches
     * (or adding new branches) if the spatial is not connected to the root OR
     * if the model is flat (vs usDepth) then the ancestor is not needed, but
     * collisions are more likely to occur
     */
    Spatial ancestor = getTopNode(spatial);// <- if the spatial is connected to
                                           // the root
    nodes.putAll(buildTree(nodes, "", ancestor, includeGeometries, useDepth));
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

  public void rename(String name, String newName) {
    UserData data = getUserData(name);
    if (data == null) {
      error("rename(%s, %s) could not find %s", name, newName, name);
    }

    // change name
    data.spatial.setName(newName);
    nodes.remove(name);
    // fix index
    nodes.put(newName, data.spatial);
  }

  public void setRotation(String name, String rotation) {
    UserData o = getUserData(name);
    if (o == null) {
      error("setRotation %s could not be found", name);
      return;
    }
    o.rotationMask = util.getUnitVector(rotation);
  }

  public void attachChild(Spatial node) {
    rootNode.attachChild(node);
  }

  public Node getRootNode() {
    return rootNode;
  }

  public AppSettings getSettings() {
    return settings;
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

  public Map<String, Spatial> buildTree() {
    TreeMap<String, Spatial> tree = new TreeMap<String, Spatial>();
    return buildTree(tree, "", rootNode, false, false);
  }

  AtomicInteger id = new AtomicInteger();

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
  public Map<String, Spatial> buildTree(Map<String, Spatial> tree, String path, Spatial spatial, boolean includeGeometries, boolean useDepthKeys) {
    if (useDepthKeys) {
      path = path + KEY_SEPERATOR + spatial.getName();
    } else {
      path = spatial.getName();
    }
    if (tree.containsKey(path)) {
      Spatial s = tree.get(path);
      if (spatial != s) {
        // if (s instanceof Geometry) {
        s.setName(String.format("%s-%d", s.getName(), id.incrementAndGet()));
        // } else {
        // log.error("buildTree collision [{}] {} already exists - NOT replacing
        // with {}", path, s, spatial);
        // return tree;
        // }
      }
      // already exists with correct path
    }

    if (!includeGeometries && (spatial instanceof Geometry)) {
      return tree;
    }

    // putting both nodes & geometries on the tree
    tree.put(path, spatial);

    if (spatial instanceof Node) {
      List<Spatial> children = ((Node) spatial).getChildren();
      for (int i = 0; i < children.size(); ++i) {
        buildTree(tree, path, children.get(i), includeGeometries, useDepthKeys);
      }
    }
    return tree;
  }

  public Spatial getSelected() {
    return selected;
  }

  public void saveSpatial(Spatial toSave) {
    try {
      String filename = toSave.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".txt";
      TreeMap<String, Spatial> tree = new TreeMap<String, Spatial>();
      buildTree(tree, "", toSave, false, true);
      // String ret = CodecUtils.toJson(tree);
      FileOutputStream fos = new FileOutputStream(filename);
      for (String key : tree.keySet()) {
        String type = (tree.get(key) instanceof Node) ? " (Node)" : " (Geometry)";
        fos.write(String.format("%s\n", key + type).getBytes());
      }
      fos.close();
    } catch (Exception e) {
      error(e);
    }
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
    if (nameMappings.containsKey(path)) {
      path = nameMappings.get(path);
    }
    if (!nodes.containsKey(path)) {
      error("geteUserData %s cannot be found", path);
      return null;
    }
    Spatial spatial = nodes.get(path);
    UserData userData = spatial.getUserData("data");
    if (userData == null) {
      userData = new UserData(this, spatial);
    }
    return userData;
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

  @Override
  public void visit(Spatial spatial) {
    // TODO Auto-generated method stub

  }

  public Node createUnitAxis() {
    return util.createUnitAxis();
  }

  public Geometry createBoundingBox(Spatial spatial) {
    return util.createBoundingBox(spatial);
  }

}