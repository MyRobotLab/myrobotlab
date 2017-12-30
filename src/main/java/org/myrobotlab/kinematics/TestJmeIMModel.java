package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.jme3.interfaces.IntegratedMovementInterface;
import org.myrobotlab.service.Servo.IKData;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
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

/**
 * @author Christian
 *
 */
public class TestJmeIMModel extends SimpleApplication implements IntegratedMovementInterface{
  private transient HashMap<String, Node> nodes = new HashMap<String, Node>();
  private Queue<IKData> eventQueue = new ConcurrentLinkedQueue<IKData>();
  private transient Queue<Node> nodeQueue = new ConcurrentLinkedQueue<Node>();
  private Queue<Point> pointQueue = new ConcurrentLinkedQueue<Point>();
  private transient ArrayList<Node> collisionItems = new ArrayList<Node>();
  private boolean ready = false;
  private transient Service service;
  private transient Node point;

   
  public static void main(String[] args) {
    TestJmeIMModel app = new TestJmeIMModel();
    app.start();
  }

 @Override
  public void simpleInitApp() {
    assetManager.registerLocator("inmoov/jm3/assets", FileLocator.class);
    Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.Green);
    viewPort.setBackgroundColor(ColorRGBA.Gray);
    inputManager.setCursorVisible(true);
    flyCam.setEnabled(false);
    Node node = new Node("cam");
    node.setLocalTranslation(0, 300, 0);
    rootNode.attachChild(node);
//    ChaseCamera chaseCam = new ChaseCamera(cam, node, inputManager);
//    chaseCam.setDefaultDistance(900);
//    chaseCam.setMaxDistance(2000);
//    chaseCam.setDefaultHorizontalRotation((float)Math.toRadians(90));
//    chaseCam.setZoomSensitivity(10);
    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
    rootNode.addLight(sun);
    cam.setLocation(new Vector3f(0f,0f,900f));
    rootNode.scale(.40f);
    rootNode.setLocalTranslation(0, -200, 0);
    Cylinder c= new Cylinder(8,50,5,10,true,false);
    Geometry geom = new Geometry("Cylinder",c);
    geom.setMaterial(mat);
    point = new Node("point");
    point.attachChild(geom);
    rootNode.attachChild(point);
    ready = true;
    synchronized (service) {
      if (service!= null){
        service.notifyAll();
      }
    }
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
    inputManager.addListener(analogListener, new String[]{"Left","Right","Up","Down"});
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
  public void addPart(String name, String modelPath, float modelScale, String hookTo, Vector3f relativePosition, Vector3f rotationMask, float initialAngle) {
    Node node = new Node(name);
    if (hookTo != null) {
      //Node hookNode = nodes.get(hookTo);
      //hookNode.attachChild(node);
      node.setUserData("hookTo", hookTo);
    }
    else {
      //rootNode.attachChild(node);
      node.setUserData("hookTo", "rootNode");
    }
    if (modelPath != null) {
      Spatial spatial= assetManager.loadModel(modelPath);
      spatial.scale(modelScale);
      spatial.setName(name);
      node.attachChild(spatial);
    }
    node.setLocalTranslation(relativePosition);
    Vector3f angle = rotationMask.mult(initialAngle);
    node.rotate(angle.x, angle.y, angle.z);
    node.setUserData("rotationMask_x", rotationMask.x);
    node.setUserData("rotationMask_y", rotationMask.y);
    node.setUserData("rotationMask_z", rotationMask.z);
    float pos = 0.0f;
    node.setUserData("currentAngle",  pos);
    nodes.put(name, node);
    nodeQueue.add(node);
  }
  
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
      if (nodes.containsKey(event.name)){
        Node node = nodes.get(event.name);
        Vector3f rotMask = new Vector3f((float) node.getUserData("rotationMask_x"), (float) node.getUserData("rotationMask_y"), (float) node.getUserData("rotationMask_z"));
        float currentAngle = (float) node.getUserData("currentAngle");
        double rotation = (event.pos-currentAngle)*Math.PI/180;
        Vector3f angle = rotMask.mult((float)rotation);
        node.rotate(angle.x, angle.y, angle.z);
        node.setUserData("currentAngle", event.pos.floatValue());
        nodes.put(event.name, node);
      }
      
    }
    while (pointQueue.size() > 0) {
      Point p = pointQueue.remove();
      point.setLocalTranslation((float)p.getX(), (float)p.getZ(), (float)p.getY());
    }
  }
  
  public boolean isReady() {
    return ready;
  }

  @Override
  public void setService(Service integratedMovement) {
    service = integratedMovement;
    
  }
  public ActionListener actionListener = new ActionListener() {
    public void onAction(String name, boolean keyPressed, float tpf) {
      if (name.equals("MouseClickL")) {
        //rotate+= keyPressed;
        rootNode.rotate(0, 1, 0);
        //Log.info(rotate);
      }
//      if (name.equals("Rotate")) {
//        Vector3f camloc = cam.getLocation();
//        camloc.x += 10;
//        cam.setLocation(camloc);
//      }
       /** TODO: test for mapping names and implement actions */
    }
  };
  
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
      else if (name.equals("MMouseUp")){
        rootNode.setLocalScale(rootNode.getLocalScale().mult(1.05f));
      }
      else if (name.equals("MMouseDown")){
        rootNode.setLocalScale(rootNode.getLocalScale().mult(0.95f));
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
  private HashMap<String, Geometry> shapes = new HashMap<String, Geometry>();
  private boolean updateCollisionItem = false;


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
      shapes.put(item.name, geom);
      geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, ori, end));
      geom.lookAt(end, Vector3f.UNIT_Y);
      //geom.scale(0.5f);
      Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
      if (item.fromKinect) {
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

  public void addObject(ConcurrentHashMap<String, CollisionItem> items) {
    updateCollisionItem  = true;
    for (CollisionItem item:items.values()) {
      addObject(item);
    }
    updateCollisionItem = false;
  }

  public void addPoint(Point point) {
    pointQueue.add(point);
    
  }


 
}
