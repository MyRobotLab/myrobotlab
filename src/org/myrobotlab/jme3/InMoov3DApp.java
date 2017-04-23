package org.myrobotlab.jme3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.jme3.interfaces.IntegratedMovementInterface;
import org.myrobotlab.kinematics.CollisionItem;
import org.myrobotlab.kinematics.Map3DPoint;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Servo.IKData;
import org.python.jline.internal.Log;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;


/**
 * @author Christian
 *
 */
public class InMoov3DApp extends SimpleApplication implements IntegratedMovementInterface{
  private transient HashMap<String, Node> nodes = new HashMap<String, Node>();
  private Queue<IKData> eventQueue = new ConcurrentLinkedQueue<IKData>();
  private transient HashMap<String, Node> servoToNode = new HashMap<String, Node>();
  private HashMap<String, Mapper> maps = new HashMap<String, Mapper>();
  private transient Service service = null;
  private transient Queue<Node> nodeQueue = new ConcurrentLinkedQueue<Node>();
  private HashMap<String, Geometry> shapes = new HashMap<String, Geometry>();
  private boolean updateCollisionItem;
  private Queue<Point> pointQueue = new ConcurrentLinkedQueue<Point>();
  private transient Node point;
  private transient ArrayList<Node> collisionItems = new ArrayList<Node>();


   
  public static void main(String[] args) {
    InMoov3DApp app = new InMoov3DApp();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1024,960);
    //settings.setEmulateMouse(false);
    // settings.setUseJoysticks(false);
    settings.setUseInput(false);
    app.setSettings(settings);
    app.setShowSettings(false);
    app.setPauseOnLostFocus(false);
    app.start();
  }

  @Override
  public void simpleInitApp() {
    assetManager.registerLocator("inmoov/jm3/assets", FileLocator.class);

    inputManager.setCursorVisible(true);
    flyCam.setEnabled(false);
    cam.setLocation(new Vector3f(0f,0f,900f));

    inputManager.addMapping("MouseClickL", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(analogListener, "MouseClickL");
    inputManager.addMapping("MouseClickR", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(analogListener, "MouseClickR");
    inputManager.addMapping("MMouseUp", new MouseAxisTrigger(MouseInput.AXIS_WHEEL,false));
    inputManager.addListener(analogListener, "MMouseUp");
    inputManager.addMapping("MMouseDown", new MouseAxisTrigger(MouseInput.AXIS_WHEEL,true));
    inputManager.addListener(analogListener, "MMouseDown");
    inputManager.addMapping("Left",  new KeyTrigger(KeyInput.KEY_A),
        new KeyTrigger(KeyInput.KEY_LEFT)); // A and left arrow
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D),
        new KeyTrigger(KeyInput.KEY_RIGHT)); // D and right arrow    
    inputManager.addMapping("Up",  new KeyTrigger(KeyInput.KEY_W),
        new KeyTrigger(KeyInput.KEY_UP)); // A and left arrow
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S),
        new KeyTrigger(KeyInput.KEY_DOWN)); // D and right arrow    
    inputManager.addMapping("ZoomIn",  new KeyTrigger(KeyInput.KEY_E));
    inputManager.addMapping("ZoomOut",  new KeyTrigger(KeyInput.KEY_Q));
    inputManager.addListener(analogListener, new String[]{"Left","Right","Up","Down", "ZoomIn","ZoomOut"});

    
    viewPort.setBackgroundColor(ColorRGBA.Gray);

    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
    rootNode.addLight(sun);
    rootNode.scale(.5f);
    rootNode.setLocalTranslation(0, -200, 0);
    
    Node node = new Node("ltorso");
    rootNode.attachChild(node);
    Spatial spatial = assetManager.loadModel("Models/ltorso.j3o");
    spatial.setName("ltorso");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(0,0,0));
    Vector3f  rotationMask = Vector3f.UNIT_X.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("ltorso", node);
    
    node = new Node("mtorso");
    Node parentNode = nodes.get("ltorso");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/mtorso.j3o");
    spatial.setName("mtorso");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(0,0,0));
    rotationMask = Vector3f.UNIT_Y.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("mtorso", node);
    maps.put("mtorso", new Mapper(0,180,15,165));
    
    node = new Node("ttorso");
    parentNode = nodes.get("mtorso");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/ttorso1.j3o");
    spatial.setName("mtorso");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(0,105,10));
    rotationMask = Vector3f.UNIT_Z.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("ttorso", node);
    maps.put("ttorso", new Mapper(0,180,80,100));
    
    node = new Node("rightS");
    parentNode = nodes.get("ttorso");
    parentNode.attachChild(node);
    node.setLocalTranslation(new Vector3f(0,300,0));
    rotationMask = Vector3f.UNIT_Z.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("rightS", node);
    
    node = new Node("Romoplate");
    parentNode = nodes.get("rightS");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/Romoplate1.j3o");
    spatial.setName("Romoplate");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(-143,0,-17));
    rotationMask = Vector3f.UNIT_Z.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    Vector3f angle = rotationMask.mult((float)Math.toRadians(6));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("Romoplate", node);
    maps.put("Romoplate", new Mapper(0,180,10,70));

    node = new Node("Rshoulder");
    parentNode = nodes.get("Romoplate");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/Rshoulder1.j3o");
    spatial.setName("Rshoulder");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(-23,-45,0));
    rotationMask = Vector3f.UNIT_X.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(-2));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("Rshoulder", node);
    maps.put("Rshoulder", new Mapper(0,180,0,180));

    node = new Node("Rrotate");
    parentNode = nodes.get("Rshoulder");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/rotate1.j3o");
    spatial.setName("Rrotate");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(-57,-55,8));
    rotationMask = Vector3f.UNIT_Y.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("Rrotate", node);
    maps.put("Rrotate", new Mapper(0,180,46,160));

    node = new Node("Rbicep");
    parentNode = nodes.get("Rrotate");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/Rbicep1.j3o");
    spatial.setName("Rbicep");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(5,-225,-32));
    rotationMask = Vector3f.UNIT_X.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(30));
    node.rotate(angle.x, angle.y, angle.z);
    //node.rotateUpTo(angle);
    nodes.put("Rbicep", node);
    maps.put("Rbicep", new Mapper(0,180,5,60));

    node = new Node("leftS");
    parentNode = nodes.get("ttorso");
    parentNode.attachChild(node);
    node.setLocalTranslation(new Vector3f(0,300,0));
    rotationMask = Vector3f.UNIT_Z.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("leftS", node);

    node = new Node("omoplate");
    parentNode = nodes.get("leftS");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/Lomoplate1.j3o");
    spatial.setName("omoplate");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(143,0,-15));
    rotationMask = Vector3f.UNIT_Z.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(4));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("omoplate", node);
    maps.put("omoplate", new Mapper(0,180,10,70));

    node = new Node("shoulder");
    parentNode = nodes.get("omoplate");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/Lshoulder.j3o");
    spatial.setName("shoulder");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(17,-45,5));
    rotationMask = Vector3f.UNIT_X.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("shoulder", node);
    maps.put("shoulder", new Mapper(0,180,0,180));

    node = new Node("rotate");
    parentNode = nodes.get("shoulder");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/rotate1.j3o");
    spatial.setName("rotate");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(65,-58,-3));
    rotationMask = Vector3f.UNIT_Y.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("rotate", node);
    maps.put("rotate", new Mapper(0,180,46,180));

    node = new Node("bicep");
    parentNode = nodes.get("rotate");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/Lbicep.j3o");
    spatial.setName("bicep");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(-14,-223,-28));
    rotationMask = Vector3f.UNIT_X.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(27));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("bicep", node);
    maps.put("bicep", new Mapper(0,180,5,60));

    node = new Node("RWrist");
    parentNode = nodes.get("Rbicep");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/RWristFinger.j3o");
    spatial.setName("RWrist");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(15,-290,-10));
    rotationMask = Vector3f.UNIT_Y.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(-90));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("RWrist", node);
    maps.put("RWrist", new Mapper(0,180,0,180));
    
    node = new Node("LWrist");
    parentNode = nodes.get("bicep");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/LWristFinger.j3o");
    spatial.setName("LWrist");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(0,-290,-20));
    rotationMask = Vector3f.UNIT_Y.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(-90));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("LWrist", node);
    maps.put("LWrist", new Mapper(0,180,0,180));

    node = new Node("neck");
    parentNode = nodes.get("ttorso");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/neck.j3o");
    spatial.setName("neck");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(0,452.5f,-45));
    rotationMask = Vector3f.UNIT_X.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("neck", node);
    maps.put("neck", new Mapper(0,180,-45,45));

    node = new Node("rollNeck");
    parentNode = nodes.get("neck");
    parentNode.attachChild(node);
    node.setLocalTranslation(new Vector3f(0,0,0));
    rotationMask = Vector3f.UNIT_Z.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(2));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("rollNeck", node);
    maps.put("rollNeck", new Mapper(0,180,60,115));
    
    node = new Node("head");
    parentNode = nodes.get("rollNeck");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/head.j3o");
    spatial.setName("head");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(0,10f,20));
    rotationMask = Vector3f.UNIT_Y.mult(-1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    nodes.put("head", node);
    maps.put("head", new Mapper(0,180,30,150));

    node = new Node("jaw");
    parentNode = nodes.get("head");
    parentNode.attachChild(node);
    spatial = assetManager.loadModel("Models/jaw.j3o");
    spatial.setName("neck");
    node.attachChild(spatial);
    node.setLocalTranslation(new Vector3f(-5,63f,-50));
    rotationMask = Vector3f.UNIT_X.mult(1);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    node.setUserData("currentAngle",  0);
    angle = rotationMask.mult((float)Math.toRadians(5));
    node.rotate(angle.x, angle.y, angle.z);
    nodes.put("jaw", node);
    maps.put("jaw", new Mapper(0,180,60,90));

    Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.Green);
    Cylinder c= new Cylinder(4,10,5,10,true,false);
    Geometry geom = new Geometry("Cylinder",c);
    geom.setMaterial(mat);
    point = new Node("point");
    point.attachChild(geom);
    rootNode.attachChild(point);

    if (service!=null) {
      synchronized(service) {
        service.notifyAll();
      }
    }
    
}

  /**
   * 
   * @param name : name of the part
   * @param modelPath : path leading the the 3dmesh (null for no model)
   * @param modelScale : model will be scale to this parameter
   * @param hookTo : attach this part to the hook part (null to hook to the root)
   * @param relativePosition : position relative to the hook part
   * @param rotationMask : set Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z) for the axe of rotation
   * @param initialAngle : initial angle of rotation of the part (in radian)
   */
  
  public void updatePosition(IKData event){
    eventQueue.add(event) ;
  }

  public void simpleUpdate(float tpf) {
    if (updateCollisionItem) {
      for (Node node : collisionItems) {
        if (node.getUserData("collisionItem") !=null){
          node.removeFromParent();
          node.updateGeometricState();
        }
      }
      collisionItems.clear();
    }
    while (nodeQueue.size() > 0) {
      Node node = nodeQueue.remove();
      Node hookNode = nodes.get(node.getUserData("hookTo"));
      if (hookNode == null) {
        hookNode = rootNode;
      }
      Spatial x = hookNode.getChild(node.getName());
      if (x != null) {
        rootNode.updateGeometricState();
      }
      hookNode.attachChild(node);
      if (node.getUserData("collisionItem") != null) {
        collisionItems.add(node);
      }
    }
    while (eventQueue.size() > 0) {
      IKData event = eventQueue.remove();
      if (servoToNode.containsKey(event.name)){
        Node node = servoToNode.get(event.name);
        Vector3f rotMask = new Vector3f((float) node.getUserData("rotationMask_x"), (float) node.getUserData("rotationMask_y"), (float) node.getUserData("rotationMask_z"));
        float currentAngle = (float) node.getUserData("currentAngle");
        Mapper map = maps.get(node.getName());
        float rotation = (float) ((map.calcOutput(event.pos))*Math.PI/180 - currentAngle*Math.PI/180);
        Vector3f angle = rotMask.mult((float)rotation);
        node.rotate(angle.x, angle.y, angle.z);
        node.setUserData("currentAngle", (float)(map.calcOutput(event.pos)));
        servoToNode.put(event.name, node);
        nodes.put(node.getName(), node);
      }
    }
    while (pointQueue.size() > 0) {
      Point p = pointQueue.remove();
      point.setLocalTranslation((float)p.getX(), (float)p.getZ(), (float)p.getY());
    }
  }
  
  public void addServo(String partName, Servo servo) {
    if (nodes.containsKey(partName)){
      Node node = nodes.get(partName);
      Mapper map = maps.get(partName);
      map.setMinMaxInput(servo.getMinInput(), servo.getMaxInput());
      double angle = -map.calcOutput(servo.getRest()) + map.calcOutput(servo.getCurrentPos());
      angle  *=  Math.PI/180;
      Vector3f rotMask = new Vector3f((float) node.getUserData("rotationMask_x"), (float) node.getUserData("rotationMask_y"), (float) node.getUserData("rotationMask_z"));
      Vector3f rotAngle = rotMask.mult((float)angle);
      node.rotate(rotAngle.x, rotAngle.y, rotAngle.z);
      node.setUserData("currentAngle", (float)map.calcOutput(servo.getCurrentPos()));
      nodes.put(partName, node);
      servoToNode.put(servo.getName(), node);
      maps.put(partName, map);
    }
    else {
      Log.info(partName + " is not a valid part name for VinMoov");
    }
  }
  
  public void setService(Service service) {
    this.service = service;
  }
  
  private AnalogListener analogListener = new AnalogListener() {
    public void onAnalog(String name, float keyPressed, float tpf) {
      if (name.equals("MouseClickL")) {
        //rotate+= keyPressed;
        rootNode.rotate(0, -keyPressed, 0);
        //Log.info(rotate);
      }
      else if (name.equals("MouseClickR")) {
        //rotate+= keyPressed;
        rootNode.rotate(0, keyPressed, 0);
        //Log.info(rotate);
      }
      else if (name.equals("MMouseUp") || name.equals("ZoomIn")){
        rootNode.setLocalScale(rootNode.getLocalScale().mult(1.01f));
      }
      else if (name.equals("MMouseDown") || name.equals("ZoomOut")){
        rootNode.setLocalScale(rootNode.getLocalScale().mult(0.99f));
      }
      else if (name.equals("Up")){
        rootNode.move(0, keyPressed*100, 0);
      }
      else if (name.equals("Down")){
        rootNode.move(0, -keyPressed*100, 0);
      }
      else if (name.equals("Left")){
        rootNode.move(-keyPressed*100, 0, 0);
      }
      else if (name.equals("Right")){
        rootNode.move(keyPressed*100, 0, 0);
      }
    }
  };

  @Override
  public void addObject(CollisionItem item) {
    if (!item.isRender()) {
      return;
    }
    if (item.isFromKinect()){
      Node pivot = new Node(item.getName());
      for(Map3DPoint p : item.cloudMap.values()) {
        Box b = new Box(4f, 4f, 4f);
        Geometry geo = new Geometry("Box",b);
        Vector3f pos = new Vector3f((float)p.point.getX(), (float)p.point.getZ(), (float)p.point.getY());
        geo.setLocalTranslation(pos);
        Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        geo.setMaterial(mat);
        pivot.attachChild(geo);
      }
      pivot.setUserData("HookTo", null);
      pivot.setUserData("collisionItem", "1");
      nodeQueue.add(pivot);
    }
    else {
      Vector3f ori = new Vector3f((float)item.getOrigin().getX(), (float)item.getOrigin().getZ(), (float)item.getOrigin().getY());
      Vector3f end = new Vector3f((float)item.getEnd().getX(), (float)item.getEnd().getZ(), (float)item.getEnd().getY());
      Cylinder c= new Cylinder(8,50,(float)item.getRadius(),(float)item.getLength(),true,false);
      Geometry geom = new Geometry("Cylinder",c);
      shapes.put(item.getName(), geom);
      geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, ori, end));
      geom.lookAt(end, Vector3f.UNIT_Y);
      //geom.scale(0.5f);
      Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
      if (item.isFromKinect()) {
        mat.setColor("Color", ColorRGBA.Red);
      }
      else {
        mat.setColor("Color", ColorRGBA.Blue);
      }
      geom.setMaterial(mat);
      Node pivot = new Node(item.getName());
      pivot.attachChild(geom);
      pivot.setUserData("HookTo", null);
      pivot.setUserData("collisionItem", "1");
      nodeQueue.add(pivot);
    }
  }

  @Override
  public void addObject(ConcurrentHashMap<String, CollisionItem> items) {
    updateCollisionItem  = true;
    for (CollisionItem item:items.values()) {
      addObject(item);
    }
    updateCollisionItem = false;
  }

  @Override
  public void addPoint(Point point) {
    pointQueue.add(point);
  }
  
  public void setMinMaxAngles(String partName, double min, double max){
    if (maps.containsKey(partName)){
      Mapper map = maps.get(partName);
      map = new Mapper(map.getMinX(), map.getMaxX(), min, max);
      maps.put(partName, map);
    }
    else {
      Log.info("No part named "+ partName + " found");
    }
  }

}
