package org.myrobotlab.IntegratedMovement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.myrobotlab.kinematics.Point;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

/**
 * @author Christian
 *
 */
public class JmeIMModel extends SimpleApplication {
  private transient Queue<Node> nodeQueue = new ConcurrentLinkedQueue<Node>();
  private Queue<Point> pointQueue = new ConcurrentLinkedQueue<Point>();
  private transient ArrayList<Node> collisionItems = new ArrayList<Node>();
  private transient JmeManager service;
  private transient Node point;

  @Override
  public void simpleInitApp() {
    service.simpleInitApp();
    synchronized (service) {
      if (service != null) {
        service.notifyAll();
      }
    }
 }

  public void simpleUpdate(float tpf) {
    if (updateCollisionItem) {
      for (Node node : collisionItems) {
        if (node.getUserData("collisionItem") != null) {
          node.removeFromParent();
          node.updateGeometricState();
        }
      }
      collisionItems.clear();
    }
    while (pointQueue.size() > 0) {
      Point p = pointQueue.remove();
      point.setLocalTranslation((float) p.getX(), (float) p.getZ(), (float) p.getY());
    }
    service.simpleUpdate(tpf);
  }

  public void setService(JmeManager jmeManager2) {
    service = jmeManager2;

  }

  private HashMap<String, Geometry> shapes = new HashMap<String, Geometry>();
  private boolean updateCollisionItem = false;

  public void addObject(CollisionItem item) {
    if (!item.isRender()) {
      return;
    }
    if (item.isFromKinect()) {
      Node pivot = new Node(item.getName());
      for (Map3DPoint p : item.cloudMap.values()) {
        Box b = new Box(4f, 4f, 4f);
        Geometry geo = new Geometry("Box", b);
        Vector3f pos = new Vector3f((float) p.point.getX(), (float) p.point.getZ(), (float) p.point.getY());
        geo.setLocalTranslation(pos);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        geo.setMaterial(mat);
        pivot.attachChild(geo);
      }
      pivot.setUserData("HookTo", null);
      pivot.setUserData("collisionItem", "1");
      nodeQueue.add(pivot);
    } else {
      Vector3f ori = new Vector3f((float) item.getOrigin().getX(), (float) item.getOrigin().getZ(), (float) item.getOrigin().getY());
      Vector3f end = new Vector3f((float) item.getEnd().getX(), (float) item.getEnd().getZ(), (float) item.getEnd().getY());
      Cylinder c = new Cylinder(8, 50, (float) item.getRadius(), (float) item.getLength(), true, false);
      Geometry geom = new Geometry("Cylinder", c);
      shapes.put(item.name, geom);
      geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, ori, end));
      geom.lookAt(end, Vector3f.UNIT_Y);
      // geom.scale(0.5f);
      Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
      if (item.fromKinect) {
        mat.setColor("Color", ColorRGBA.Red);
      } else {
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
    updateCollisionItem = true;
    for (CollisionItem item : items.values()) {
      addObject(item);
    }
    updateCollisionItem = false;
  }

  public void addPoint(Point point) {
    pointQueue.add(point);

  }


}
