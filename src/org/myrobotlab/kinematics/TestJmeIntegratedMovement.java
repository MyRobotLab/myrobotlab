/**
 * 
 */
package org.myrobotlab.kinematics;

import java.util.HashMap;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;

/**
 * @author Christian
 *
 */
public class TestJmeIntegratedMovement extends SimpleApplication {
	private HashMap<String, CollisionItem> objects;

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
			Cylinder c= new Cylinder(8,50,(float)ci.getRadius()/2,(float)ci.getLength()/2,true,false);
			Geometry geom = new Geometry("Cylinder",c);
			Vector3f ori = new Vector3f((float)ci.getOrigin().getX()/2, (float)ci.getOrigin().getZ()/2, (float)ci.getOrigin().getY()/2);
			Vector3f end = new Vector3f((float)ci.getEnd().getX()/2, (float)ci.getEnd().getZ()/2, (float)ci.getEnd().getY()/2);
			geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, ori, end));
			geom.lookAt(end, Vector3f.UNIT_Y);
			//geom.scale(0.5f);
			Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
			mat.setColor("Color", ColorRGBA.Blue);
			geom.setMaterial(mat);
			Node pivot = new Node("pivot");
			rootNode.attachChild(pivot);
			pivot.attachChild(geom);
//			pivot.rotate(0.4f, 0.4f, 0.0f);
		}
		this.cam.setLocation(new Vector3f(0f,0f,1000f));
	}
	
	public static void main(String[] args) {
		TestJmeIntegratedMovement app = new TestJmeIntegratedMovement();
		app.start();
	}

	public void setObjects(HashMap<String, CollisionItem> collisionObject) {
		objects = collisionObject;
		
	}

}
