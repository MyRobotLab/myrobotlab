package org.myrobotlab.kinematics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.service.Servo.IKData;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * @author Christian
 *
 */
public class TestJmeIMModel extends SimpleApplication{
  private HashMap<String, Node> nodes = new HashMap<String, Node>();
  private Queue<IKData> eventQueue = new ConcurrentLinkedQueue<IKData>();
  private Queue<Node> nodeQueue = new ConcurrentLinkedQueue<Node>();

   
  public static void main(String[] args) {
    TestJmeIMModel app = new TestJmeIMModel();
    app.start();
  }

  public void setObjects(HashMap<String, CollisionItem> collisionObject) {
    
  }
  
  @Override
  public void simpleInitApp() {
    assetManager.registerLocator("src/resource/jme", FileLocator.class);
    Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.Red);
   
//    Spatial teapot = assetManager.loadModel("inMoov/mtorso.j3o");
//    Material mat_default = new Material(
//        assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
//    //teapot.setMaterial(mat_default);
//    teapot.setLocalScale(600f);
//    Node n1 = new Node("n1");
//    n1.attachChild(teapot);
//    rootNode.attachChild(n1);
//    
//    Spatial chest = assetManager.loadModel("inMoov/ttorso.j3o");
//    //chest.setMaterial(mat_default);
//    Node n2 = new Node("n2");
//    chest.setLocalScale(600);
//    n2.attachChild(chest);
//    n1.attachChild(n2);
//    n2.setLocalTranslation(0, 113*0.59f, 0);
//    
//    Node n3 = new Node("n3");
//    //n3.setLocalScale(600);
//    n2.attachChild(n3);
//    n3.setLocalTranslation(0, 315*0.57f, 0);
//    
//    
//    Spatial Romoplate = assetManager.loadModel("inMoov/Romoplate.j3o");
//    Node n4 = new Node("n4");
//    n4.attachChild(Romoplate);
//    n3.attachChild(n4);
//    //rootNode.attachChild(n4);
//    Romoplate.setLocalScale(600);
//    n4.setLocalTranslation(-143*0.60f, 0, 0);
//    Vector3f test1 = n1.getWorldScale();
//    Vector3f test2 = n2.getWorldScale();
//    Vector3f test3 = n4.getWorldScale();
    
    
    //n1.rotate(0, 3.1416f/2, 3.1416f/2);
//    n2.rotate(0, 0, 3.1416f/16);
//    n2.rotate(0, 0, -3.1416f/16);
//    n4.rotate(0,0,0);
    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
    rootNode.addLight(sun);
    this.cam.setLocation(new Vector3f(0f,0f,900f));
    rootNode.scale(1.0f);
    rootNode.setLocalTranslation(0, 00, 0);
    
    
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
    while (nodeQueue.size() > 0) {
      Node node = nodeQueue.remove();
      Node hookNode = nodes.get(node.getUserData("hookTo"));
      if (hookNode == null) {
        rootNode.attachChild(node);
      }
      else {
        hookNode.attachChild(node);
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
  }
}
