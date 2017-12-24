/**
 * 
 */
package org.myrobotlab.kinematics;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

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
public class TestJmeIntegratedMovement extends SimpleApplication {
	private ConcurrentHashMap<String, CollisionItem> objects;
	private Vector3f camLocation;
	private int camXDir = 1;
	private int camZDir = -1;
	private HashMap<String, Geometry> shapes = new HashMap<String, Geometry>();
	private boolean updateShape = false;

	@Override
	public void simpleInitApp() {
//		if (true) {
//			Cylinder c= new Cylinder(8,50,50,10,true,false);
//			Geometry geom = new Geometry("Cylinder",c);
//			//Vector3f ori = new Vector3f((float)ci.getOrigin().getX(), (float)ci.getOrigin().getZ(), (float)ci.getOrigin().getY());
//			//Vector3f end = new Vector3f((float)ci.getEnd().getX(), (float)ci.getEnd().getZ(), (float)ci.getEnd().getY());
//			//geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, ori, end));
//			//geom.lookAt(end, Vector3f.UNIT_Y);
//			Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
//			mat.setColor("Color", ColorRGBA.Blue);
//			geom.setMaterial(mat);
//			Node pivot = new Node("pivot");
//			rootNode.attachChild(pivot);
//			pivot.attachChild(geom);
//
//		}
		for (CollisionItem ci : objects.values()) {
			Vector3f ori = new Vector3f((float)ci.getOrigin().getX()/2, (float)ci.getOrigin().getZ()/2, (float)ci.getOrigin().getY()/2);
			Vector3f end = new Vector3f((float)ci.getEnd().getX()/2, (float)ci.getEnd().getZ()/2, (float)ci.getEnd().getY()/2);
			if (!ci.fromKinect) {
				Cylinder c= new Cylinder(8,50,(float)ci.getRadius()/2,(float)ci.getLength()/2,true,false);
				Geometry geom = new Geometry("Cylinder",c);
				shapes.put(ci.name, geom);
				geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, ori, end));
				geom.lookAt(end, Vector3f.UNIT_Y);
				//geom.scale(0.5f);
				Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
				if (ci.fromKinect) {
					mat.setColor("Color", ColorRGBA.Red);
				}
				else {
					mat.setColor("Color", ColorRGBA.Blue);
				}
				geom.setMaterial(mat);
				Node pivot = new Node("pivot");
				rootNode.attachChild(pivot);
				pivot.attachChild(geom);
//			pivot.rotate(0.4f, 0.4f, 0.0f);
			}
			else {
				Node item = new Node("item");
				for(Map3DPoint p : ci.cloudMap.values()) {
					Box b = new Box(1.5f, 1.5f, 1.5f);
					Geometry geo = new Geometry("Box",b);
					Vector3f pos = new Vector3f((float)p.point.getX()/2, (float)p.point.getZ()/2, (float)p.point.getY()/2);
					geo.setLocalTranslation(pos);
					Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
					mat.setColor("Color", ColorRGBA.Red);
					geo.setMaterial(mat);
					item.attachChild(geo);
					
				}
				rootNode.attachChild(item);
			}
		}
		this.cam.setLocation(new Vector3f(0f,0f,1000f));
		//this.cam.lookAtDirection(new Vector3f(0,0,0), new Vector3f(0,0,0));
	}
	
	public static void main(String[] args) {
		TestJmeIntegratedMovement app = new TestJmeIntegratedMovement();
		app.start();
	}

	public void setObjects(ConcurrentHashMap<String, CollisionItem> collisionObject) {
		objects = collisionObject;
		
	}
	
	@Override
	public void simpleUpdate(float tpf) {
		Vector3f camLoc = cam.getLocation();
		if (camLoc.x >= 1000){
			camXDir = -1;
		}
		if (camLoc.x <= -1000) {
			camXDir = 1;
		}
		if (camLoc.z >= 1000){
			camZDir = -1;
		}
		if (camLoc.z <= -1000) {
			camZDir = 1;
		}
		//camLoc.add(2*tpf*camXDir, 0, 2*tpf*camZDir);
		camLoc.x += 50*tpf*camXDir;
		camLoc.z += 50*tpf*camZDir;
		//this.cam.setLocation(camLoc);
		//cam.lookAtDirection(new Vector3f(camLoc.x*-1,0,camLoc.z*-1), cam.getUp());
		if (updateShape ) {
			for (CollisionItem ci : objects.values()) {
				if (!ci.isFromKinect()) {
					Vector3f ori = new Vector3f((float)ci.getOrigin().getX()/2, (float)ci.getOrigin().getZ()/2, (float)ci.getOrigin().getY()/2);
					Vector3f end = new Vector3f((float)ci.getEnd().getX()/2, (float)ci.getEnd().getZ()/2, (float)ci.getEnd().getY()/2);
					Geometry geom = shapes.get(ci.name);
					geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, ori, end));
					geom.lookAt(end, Vector3f.UNIT_Y);
						//geom.scale(0.5f);
					shapes.put(ci.name, geom);
				}
			}
			updateShape = false;
		}
	}

	public void updateObjects(ConcurrentHashMap<String, CollisionItem> concurrentHashMap) {
		objects = concurrentHashMap;
		updateShape = true;
		//Log.info("data updated",System.currentTimeMillis());
	}

}
