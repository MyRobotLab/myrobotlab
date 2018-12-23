package org.myrobotlab.service;

import java.awt.Color;
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

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.cv.CvData;
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
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.abstracts.AbstractComputerVision;
import org.myrobotlab.virtual.VirtualMotor;
import org.slf4j.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.xml.XMLExporter;
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
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.plugins.blender.BlenderLoader;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;

public class JMonkeyEngine extends Service implements ActionListener, AnalogListener {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);

  // real JMonkeyEngine parts ...
  transient AssetManager assetManager;
  transient InputManager inputManager;
  transient FlyByCamera flyCam;
  transient Camera camera;
  transient CameraNode camNode;
  // transient AnalogListener analogListener;
  transient ViewPort viewPort;
  transient Node rootNode;
  transient AppSettings settings;
  transient Spatial control = null;
  transient Node personNode = null;

  static String color = "00FF00"; // green
  long startUpdateTs;
  long deltaMs;
  long sleepMs;
  int width = 1024;
  int height = 768;

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
    // start the jmonkey app - if you want a diferent Jme3App
    // config should be set at before this time
    start();
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

  @Override
  public void stopService() {
    super.stopService();
    try {
      stop();
    } catch (Exception e) {
      log.error("releasing jme3 app threw", e);
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

    // not really supposed to use blender models - export to j3o
    meta.addDependency("org.jmonkeyengine", "jme3-blender", jmeVersion);

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
    // give specific JmeApp the opportunity to attach
    app.attach(service);

    // Cv Publisher ..
    if (service instanceof AbstractComputerVision) {
      AbstractComputerVision cv = (AbstractComputerVision) service;
      subscribe(service.getName(), "publishCvData");
    }

    // reverse attach ?
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

  public void addBox(String boxName) {
    addBox(boxName, 3f, 2f, 3f, null, null); // room box
  }

  public void addBox(String boxName, Float width, Float depth, Float height) {
    addBox(boxName, width, depth, height, null, null);
  }

  public void addBox(String boxName, Float width, Float depth, Float height, String color, Boolean fill) {
    Box box = new Box(width, depth, height); // 1 meter square

    // wireCube.setMode(Mesh.Mode.LineLoop);
    // box.setMode(Mesh.Mode.Lines);

    Geometry geom = new Geometry(boxName, box);

    Material mat1 = null;

    if (fill == null || fill.equals(false)) {
      // mat1 = new Material(assetManager,
      // "Common/MatDefs/Light/Lighting.j3md");
      mat1 = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
    } else {
      mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
      if (color == null) {
        color = JMonkeyEngine.color;
      }
      mat1.setColor("Color", toColor(color));
    }

    // mat1 = new Material(assetManager, "Common/MatDefs/Light/Deferred.j3md");

    mat1.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Front);
    geom.setMaterial(mat1);
    rootNode.attachChild(geom);

  }

  static public ColorRGBA toColor(String userColor) {
    if (userColor == null) {
      userColor = JMonkeyEngine.color;
    }

    String clean = userColor.replace("0x", "").replace("#", "");

    ColorRGBA retColor = null;
    Color c = null;
    try {
      int cint = Integer.parseInt(clean, 16);
      c = new Color(cint);
      retColor = new ColorRGBA((float) c.getRed() / 255, (float) c.getGreen() / 255, (float) c.getBlue() / 255, 1);
      return retColor;
    } catch (Exception e) {
      log.error("creating color threw", e);
    }
    return ColorRGBA.Green;
  }

  public void simpleUpdate(float tpf) {

    // start the clock on how much time we will take
    startUpdateTs = System.currentTimeMillis();

    for (HudText hudTxt : guiText.values()) {
      hudTxt.update();
      // txt.setText("DOOD ITS ALL ABOUT THE THREAD");
    }

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

  // relative
  public void moveTo(String name, Float x, Float y, Float z) {
    Spatial spatial = rootNode.getChild(name);
    List<Spatial> childrenOfRoot = rootNode.getChildren();
    spatial.setLocalTranslation(x, y, z);
  }

  // relative move
  // favor the xy plane because we are not birds ?
  public void move(String name, Float x, Float y) {
    // must "get z"
    // move(name, x, y, null);
  }

  /**
   * load all files
   */
  public void loadJmeDir() {
    File dataDir = new File(getDataDir());
    File[] files = dataDir.listFiles();
    for (File file : files) {
      String filename = file.getName();
      int pos = filename.lastIndexOf(".");
      String ext = null;
      if (pos != -1) {
        ext = filename.substring(pos + 1).toLowerCase();
      }

      if (ext != null && ext.equals("obj")) {
        // loading file
        Spatial spatial = assetManager.loadModel(filename);
        spatial.getName();
        spatial.scale(0.05f, 0.05f, 0.05f);
        spatial.rotate(0.0f, -3.0f, 0.0f);
        spatial.setLocalTranslation(0.0f, -0.40f, -0.20f);
        rootNode.attachChild(spatial);
      }
    }
  }

  // FIXME - more parameters - location & rotation (new function "move")
  public boolean load(String filename, Float scale) {

    if (scale == null) {
      scale = 1f;
    }

    File file = getFile(filename);

    if (!file.exists()) {
      return false;
    }

    int pos = file.getName().lastIndexOf(".");
    String ext = null;
    if (pos != -1) {
      ext = file.getName().substring(pos + 1).toLowerCase();
    }

    /**
     * <pre>
     * why make a filter - give it all to assetManager to try .. oh ... except
     * json :P if (ext == null || !(ext.equals("obj") || ext.equals("blender")
     * || ext.equals("j3o") || ext.equals("json"))) { error("cannot load %s
     * unknown type", filename); return false; }
     */

    if (!ext.equals("json")) {
      // FIXME needs a "name" !!! & try/catch ?
      Spatial spatial = assetManager.loadModel(file.getName());
      spatial.getName();
      // spatial.scale(0.05f, 0.05f, 0.05f);
      // spatial.rotate(0.0f, -3.0f, 0.0f);
      // spatial.setLocalTranslation(0.0f, -0.0f, -0.0f);
      spatial.setLocalTranslation(0.0f, -1.0f, -0.0f);
      spatial.setLocalScale(scale);
      // spatial.scale(scale, scale, scale);
      rootNode.attachChild(spatial);

      return true;
    }

    try {
      String json = FileIO.toString(filename);
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

  // https://stackoverflow.com/questions/16861727/jmonkey-engine-3-0-drawing-points
  FloatBuffer pointCloudBuffer = null;
  Mesh pointCloudMesh = new Mesh();
  Material pointCloudMat = null;

  // FIXME - get then write directly to Mesh.getBuffer()
  // then set with mesh.setData(buffer)
  // https://hub.jmonkeyengine.org/t/updating-mesh-vertices/25088/7

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

  public void onPointCloud(PointCloud pc) {

    if (pc == null) {
      return;
    }
    // pointCloudMat.setBoolean("VertexColor", false);
    // pointCloudMesh.setPointSize(0.01f);

    if (pointCloudBuffer == null) {
      initPointCloud(pc);
      addBox("box-1");
      // putText("Blah", 10, 10);
      // addFloor();
      // putText("blah", 5,5,5);
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

  Node n;
  Node n2;

  public void addFloor() {
    Material matSoil = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    matSoil.setBoolean("UseMaterialColors", true);
    matSoil.setColor("Ambient", ColorRGBA.Gray);
    matSoil.setColor("Diffuse", ColorRGBA.Gray);
    matSoil.setColor("Specular", ColorRGBA.Black);
    Geometry soil = new Geometry("soil", new Box(1000, 1, 1000));
    soil.setLocalTranslation(0, -1, 0);
    soil.setMaterial(matSoil);
    rootNode.attachChild(soil);
  }

  public void setDisplayStatView(boolean b) {
    app.setDisplayStatView(b);
  }

  public void setDisplayFps(boolean b) {
    app.setDisplayFps(b);
  }

  public class HudText {
    String updateText;
    String currentText;
    String color;
    int x;
    int y;
    BitmapText node;

    private int size;

    public HudText(String text, int x, int y) {
      this.x = x;
      this.y = y;
      this.updateText = text;
      if (text == null) {
        text = "";
      }
      BitmapText txt = new BitmapText(app.loadGuiFont(), false);
      // txt.setColor(new ColorRGBA(1f, 0.1f, 0.1f, 1f));

      txt.setText(text);
      txt.setLocalTranslation(x, settings.getHeight() - y, 0);
      node = txt;
    }

    public void update() {
      if (!updateText.equals(currentText)) {
        node.setText(updateText);
        currentText = updateText;
        if (color != null) {
          node.setColor(JMonkeyEngine.toColor(color));
          node.setSize(size);
        }
      }
    }

    public void setColor(String hexString) {
      this.color = hexString;
    }

    public void setText(String text, String color, int size) {
      this.color = color;
      this.size = size;

      if (text == null) {
        text = "";
      }
      this.updateText = text;
    }

    public Node getNode() {
      return node;
    }
  }

  Map<String, HudText> guiText = new TreeMap<>();
  String fontColor = "#66ff66"; // green
  int fontSize = 14;

  public void setFontColor(String color) {
    fontColor = color;
  }

  public void setFontSize(int size) {
    fontSize = size;
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
      hud = new HudText(text, x, y);
      hud.setText(text, color, size);
      guiText.put(key, hud);
      app.getGuiNode().attachChild(hud.getNode());
    }
  }

  public void putText(String text, int x, int y) {
    putText(text, x, y, null, null);
  }

  // put 2d text into a 3d scene graph
  public void putText(String text, int x, int y, int z) {
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

  public boolean saveNode(String name) {
    try {
      Spatial spatial = rootNode.getChild(name);
      if (spatial == null) {
        error("could not save {} - node not found", name);
      }

      BinaryExporter exporter = BinaryExporter.getInstance();
      FileOutputStream out = new FileOutputStream(name + ".j3o");
      exporter.save(spatial, out);
      out.close();

      out = new FileOutputStream(name + ".xml");
      XMLExporter xmlExporter = XMLExporter.getInstance();
      xmlExporter.save(spatial, out);
      out.close();

      return true;
    } catch (Exception e) {
      log.error("exporter.save threw", e);
    }
    return false;
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

  public static void main(String[] args) {
    try {

      // FIXME - ADD DEFAULT ROTATION !!! ... default"move" & default"rotate" &
      // default rotationalMask !!!
      // FIXME - something to autoAttach ! ... even at origin position

      LoggingFactory.init(Level.INFO);

      Runtime.start("gui", "SwingGui");
      JMonkeyEngine jme = (JMonkeyEngine) Runtime.start("jme", "JMonkeyEngine");

      // jme.loadInMoov();
      // jme.load("muro.obj");

      // jme.load("Christmashat.obj", 0.01f);
      // jme.moveTo("Christmashat-geom-0", 0f, 0.70f, 0f); // wtf name !=
      // filename - shold change that

      // jme.load("Chair.obj", 0.1f);
      jme.addBox("ruler", 0.01f, 0.7747f, 0.01f, "FF0000", true);
      jme.moveTo("ruler", 0.0f, 0.7f, 0.0f);
      // jme.putText("camera x,y,z", 10, 10);
      // jme.addBox("box-1.1"); ROOM ! FIXME - normals pointing inside !!!
      // jme.load("model.dae");
      // jme.load(jme.getDataDir() + File.separator + "serwo_9g_Tower_pro.obj");
      // jme.load("cube.blend");
      // jme.load("box_realistic.obj");
      // jme.saveNode("exported.obj");
      // jme.load(jme.getDataDir() + File.separator + "cbmchhh.dae");

      boolean done = true;
      if (done) {
        return;
      }

      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
      jme.attach(cv);
      jme.putText("stat: 1\nstat: 2\nstat: 3", 10, 10);
      jme.putText("stat: 5\nstat: 6\nstat: 7", 10, 10);
      jme.putText("IS THIS OVERLAYED", 10, 10, "#FF0000");

      jme.putText("this is new text", 10, 20);
      jme.putText("this is moved new text", 100, 20);
      jme.putText("this is moved new text - replaced", 100, 20);
      // jme.subscribe("cv", "publishPointCloud");

      cv.addFilter("floor", "KinectPointCloud");

      // jme.putText("test", 5, 5, 5);
      // cv.capture("../1543648225286");
      // jme.startServoController("i01.left"); // GAH won't work :(
      // jme.startServoController("i01.right");

      //
      // Runtime.start("i01.left", "Jme3ServoController"); GAH won't work :(
      // Runtime.start("i01.right", "Jme3ServoController");
      // jme.start();

      // jme.onPointCloud(cv.getPointCloud());

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

  public void load(String name) {
    load(name, (Float) null);
  }

  @Override
  public void onAction(String name, boolean isPressed, float tpf) {
    // TODO Auto-generated method stub

  }

  public void enableFlyCam(boolean b) {
    flyCam.setEnabled(b);
  }

  // FIXME - remove - just load a json file
  // NOT TO BE CALLED BY ANY OTHER THREAD BESIDES JME THREAD !!! OR YOU GET A
  // SPATIAL EXCEPTION !
  private void loadInMoov() {

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

  @Override
  public void onAnalog(String name, float keyPressed, float tpf) {
    control = rootNode;
    // control = camNode;
    if (name.equals("MouseClickL")) {
      // rotate+= keyPressed;
      control.rotate(0, -keyPressed, 0);
      // log.info(rotate);
    } else if (name.equals("MouseClickR")) {
      // rotate+= keyPressed;
      control.rotate(0, keyPressed, 0);
      // log.info(rotate);
    } else if (name.equals("MMouseUp") || name.equals("ZoomIn")) {
      control.setLocalScale(control.getLocalScale().mult(1.0f));
    } else if (name.equals("MMouseDown") || name.equals("ZoomOut")) {
      control.setLocalScale(control.getLocalScale().mult(1.0f));
    } else if (name.equals("Up")) {
      control.move(0, keyPressed * 1, 0);
    } else if (name.equals("Down")) {
      control.move(0, -keyPressed * 1, 0);
    } else if (name.equals("Left")) {
      control.move(-keyPressed * 1, 0, 0);
    } else if (name.equals("Right")) {
      control.move(keyPressed * 1, 0, 0);
    } else if (name.equals("FullScreen")) {
      if (!fullscreen) {
        enableFullScreen(true);
      } /*else {
        enableFullScreen(false);
      }*/ // oscilatting
    }
  }

  public void enableFullScreen(boolean fullscreen) {
    this.fullscreen = fullscreen;

    if (fullscreen) {
      GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      displayMode = device.getDisplayMode();
      DisplayMode[] modes = device.getDisplayModes();
      // displayMode = modes[i]; - FIXME - change jme size draggable and full
      // screen resolution ..

      int i = 0; // note: there are usually several, let's pick the first (LET'S
                 // NOT !!! - cuz that's a dumb idea)

      // remember last dsiplay mode
      displayMode = device.getDisplayMode();
      
      settings = app.getContext().getSettings();
      log.info("settings {}", settings);
      settings.setResolution(displayMode.getWidth(), displayMode.getHeight());
      settings.setFrequency(displayMode.getRefreshRate());
      settings.setBitsPerPixel(displayMode.getBitDepth());     
      
      // settings.setFullscreen(device.isFullScreenSupported());
      settings.setFullscreen(fullscreen);
      app.setSettings(settings);
      app.restart(); // restart the context to apply changes
    } else {
      settings = app.getContext().getSettings();
      log.info("settings {}", settings);
      /*
      settings.setFrequency(displayMode.getRefreshRate());
      settings.setBitsPerPixel(displayMode.getBitDepth()); 
      */
      settings.setFullscreen(fullscreen);
      settings.setResolution(width, height);  
      app.setSettings(settings);
      app.restart();
    }
  }

  DisplayMode displayMode = null;
  boolean fullscreen = false;
  DisplayMode lastDisplayMode = null;

  public void simpleInitApp() {

    // wtf - assetManager == null - another race condition ?!?!?
    // after start - these are initialized as "default"
    assetManager = app.getAssetManager();
    inputManager = app.getInputManager();
    flyCam = app.getFlyByCamera();
    camera = app.getCamera();
    // target node to attach to camera
    personNode = new Node("person");

    rootNode = app.getRootNode();
    rootNode.attachChild(personNode);

    // rootNode.attachChild(child)

    viewPort = app.getViewPort();
    // Setting the direction to Spatial to camera, this means the camera will
    // copy the movements of the Node
    camNode = new CameraNode("cam", camera);
    camNode.setControlDir(ControlDirection.SpatialToCamera);
    camNode.lookAt(rootNode.getLocalTranslation(), Vector3f.UNIT_Y);
    // rootNode.attachChild(camNode);
    // rootNode.attachChild(cam);

    // personNode.attachChild(camNode);

    // cam.setFrustum(0, 1000, 0, 0, 0, 0);
    // cam.setFrustumNear(1.0f);

    inputManager.setCursorVisible(true);
    flyCam.setEnabled(false);
    camNode.setLocalTranslation(0, 0, 2f);
    // camera.setLocation(new Vector3f(0f, 0f, 2f));
    // cam.setLocation(new Vector3f(0f, 0f, 0f));
    // cam.setLocation(new Vector3f(0f, 0f, 900f));
    // cam.setLocation(new Vector3f(0f, 0f, 12f));
    // cam.setClipPlan);
    new File(getDataDir()).mkdirs();
    new File(getResourceDir()).mkdirs();

    assetManager.registerLocator("InMoov/jm3/assets", FileLocator.class);
    assetManager.registerLocator(getDataDir(), FileLocator.class);
    assetManager.registerLocator(getResourceDir(), FileLocator.class);
    assetManager.registerLocator("./", FileLocator.class);
    assetManager.registerLocator(getDataDir(), FileLocator.class);
    assetManager.registerLoader(BlenderLoader.class, "blend");

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
    inputManager.addMapping("MouseClickL", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(this, "MouseClickL");
    inputManager.addMapping("MouseClickR", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(this, "MouseClickR");
    inputManager.addMapping("MMouseUp", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
    inputManager.addListener(this, "MMouseUp");
    inputManager.addMapping("MMouseDown", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
    inputManager.addListener(this, "MMouseDown");
    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
    inputManager.addMapping("ZoomIn", new KeyTrigger(KeyInput.KEY_J));
    inputManager.addMapping("ZoomOut", new KeyTrigger(KeyInput.KEY_K));
    inputManager.addListener(this, new String[] { "Left", "Right", "Up", "Down", "ZoomIn", "ZoomOut" });
    // no worky
    inputManager.addMapping("FullScreen", new KeyTrigger(KeyInput.KEY_F));
    inputManager.addListener(this, "FullScreen");

    viewPort.setBackgroundColor(ColorRGBA.Gray);

    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
    // sun.setDirection(new Vector3f(-0.0f, -0.0f, -0.0f));
    rootNode.addLight(sun);

    // AmbientLight sun = new AmbientLight();
    rootNode.addLight(sun);
    // rootNode.scale(.5f);
    // rootNode.scale(1.0f);
    // rootNode.setLocalTranslation(0, -200, 0);
    rootNode.setLocalTranslation(0, 0, 0);

    // FIXME NOT i01 !! - if a InMoov Type has been identified
    // the InMoov {name} is pulled
    // FIXME - correct names of types :P
    // FIXME - INPUT ALL THIS VIA TEXT/yaml/json config !!!

    putNode("rootNode", rootNode);

    // AH HAA !!! ... so JME thread can only do this :P
    loadInMoov();
  }

}