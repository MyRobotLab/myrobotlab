/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.jme3.AnalogHandler;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.IntegratedMovement;
import org.myrobotlab.service.Log;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;


/**
 * this class will manage the JME3 app
 * @author calamity
 *
 */
public class JmeManager2 implements ActionListener {

	private TestJmeIMModel2 jmeApp;
	transient AssetManager assetManager;
	private HashMap<String, Node> nodes = new HashMap<String,Node>();
	private transient Queue<Node> nodeQueue = new ConcurrentLinkedQueue<Node>();
	private long startUpdateTs;
	private Node rootNode;
	private long deltaMs;
	private long sleepMs;
	transient AnalogListener analog = null;
	transient InputManager inputManager;
	transient IntegratedMovement im = null;
	long frameCount =0;
	
	public JmeManager2(IntegratedMovement im){
		this.im = im;
	}
	
	public void start(String string, String string2) {
	    if (jmeApp != null) {
	        //Log.info("JmeApp already started");
	        return;
	    }
	    jmeApp = new TestJmeIMModel2();
	    // jmeApp.setShowSettings(false);
	    AppSettings settings = new AppSettings(true);
	    settings.setResolution(800, 600);
	    jmeApp.setSettings(settings);
	    jmeApp.setShowSettings(false);
	    jmeApp.setPauseOnLostFocus(false);
	    jmeApp.setService(this);
	    jmeApp.start();
	    // need to wait for jmeApp to be ready or the models won't load
	    synchronized (this) {
	        try {
				wait(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	     }
	     assetManager = jmeApp.getAssetManager();
	      // add the existing objects
	      //jmeApp.addObject(collisionItems.getItems());
	}

	public void loadParts(IMData data) {
	    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	    mat.setColor("Color", ColorRGBA.Blue);
		for (IMPart part : data.getParts().values()){
		    Node node = new Node(part.getName());
		    String modelPath = part.get3DModelPath();
		    if (modelPath != null) {
		        Spatial spatial = assetManager.loadModel(modelPath);
		        spatial.scale(part.getScale());
		        spatial.setName(part.getName());
		        Node n = new Node("n");
		        n.attachChild(spatial);
		        node.attachChild(n);
			    Point ip = part.getInitialTranslateRotate();
				Quaternion i = new Quaternion().fromRotationMatrix(Util.eulerToMatrix3f(ip));
				n.setLocalRotation(i);
		    }
		    else {
		        Vector3f ori = Util.pointToVector3f(part.getOriginPoint());
		        Vector3f end = Util.pointToVector3f(part.getEndPoint());
		        Cylinder c = new Cylinder(8, 20, (float) part.getRadius(), (float) part.getLength(), true, false);
		        Geometry geom = new Geometry("Cylinder", c);
		        geom.setName(part.getName());
		        geom.setMaterial(mat);
		        Vector3f interpolate = FastMath.interpolateLinear(0.5f, ori, end);
		        //geom.lookAt(end, Vector3f.UNIT_Z);
		        Node n = new Node("n");
		        Quaternion q = new Quaternion().fromAngles(0,(float)(Math.PI/2),0);
		        n.attachChild(geom);
		        geom.setLocalRotation(q);
		        geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, new Vector3f(0,0,0), new Vector3f((float)-part.getLength(),0,0)));
		        node.attachChild(n);
			    Point ip = part.getInitialTranslateRotate();
			    //ip.add(new Point(0,0,0,0,0,90));
				Quaternion i = new Quaternion().fromRotationMatrix(Util.eulerToMatrix3f(ip));
				n.setLocalRotation(i);
		        
		    }
		    nodes.put(part.getName(), node);
		    nodeQueue.add(node);
		}
	}

	public void simpleUpdate(float tpf) {
		frameCount++;
		if (frameCount == 1) return;
	    // start the clock on how much time we will take
		startUpdateTs = System.currentTimeMillis();
	    while (nodeQueue.size() > 0) {
	        Node node = nodeQueue.remove();
	        rootNode = jmeApp.getRootNode();
	        Node pivot = new Node("pivot");
	        rootNode.attachChild(pivot);
	        pivot.attachChild(node);
	        Spatial x = rootNode.getChild(node.getName());
	        if (x != null) {
	          rootNode.updateGeometricState();
	        }
	    }
		deltaMs = System.currentTimeMillis() - startUpdateTs;
		sleepMs = 33 - deltaMs;

		if (sleepMs < 0) {
			sleepMs = 0;
		}
		try {
			Thread.sleep(sleepMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		updatePosition();
	}

	public void updatePosition() {
		IMData data = im.getData();
		for (IMPart part : data.getParts().values()){
			
			Node node = nodes.get(part.getName());
			if (node == null) continue;
			Matrix origin = part.getOrigin();
			Matrix end = part.getEnd();
			Point ini = part.getInitialTranslateRotate();
			Point o = Util.matrixToPoint(origin);
			Point e = Util.matrixToPoint(end);
			Vector3f y = new Vector3f();
			y.setX(((float)o.getX()-y.getX()+(float)ini.getX()));
			y.setY(((float)o.getY()-y.getY()+(float)ini.getY()));
			y.setZ(((float)o.getZ()-y.getZ()+(float)ini.getZ()));
			node.setLocalTranslation(y);
			Quaternion qm = new Quaternion().fromRotationMatrix(Util.matrixToMatrix3f(end));
			Quaternion i = new Quaternion().fromRotationMatrix(Util.eulerToMatrix3f(ini));
			//qm.multLocal(i);
			node.setLocalRotation(qm);
			if (part.isVisible()){
				node.setCullHint(CullHint.Never);
			}
			else {
				node.setCullHint(CullHint.Always);
			}
		}
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
	    //log.info("onAction {} {} {}", name, keyPressed, tpf);
		
	}
	private AnalogListener analogListener = new AnalogListener() {
		public void onAnalog(String name, float keyPressed, float tpf) {
			if (name.equals("MouseClickL")) {
		        // rotate+= keyPressed;
		        rootNode.rotate(0, -keyPressed, 0);
		        // Log.info(rotate);
		      } else if (name.equals("MouseClickR")) {
		        // rotate+= keyPressed;
		        rootNode.rotate(0, keyPressed, 0);
		        // Log.info(rotate);
		      } else if (name.equals("MMouseUp")) {
		        rootNode.setLocalScale(rootNode.getLocalScale().mult(1.05f));
		      } else if (name.equals("MMouseDown")) {
		        rootNode.setLocalScale(rootNode.getLocalScale().mult(0.95f));
		      } else if (name.equals("Up")) {
		        rootNode.move(0, keyPressed * 100, 0);
		      } else if (name.equals("Down")) {
		        rootNode.move(0, -keyPressed * 100, 0);
		      } else if (name.equals("Left")) {
		        rootNode.move(-keyPressed * 100, 0, 0);
		      } else if (name.equals("Right")) {
		        rootNode.move(keyPressed * 100, 0, 0);
		      }
		  }
	};

	public void simpleInitApp() {
		loadParts(im.getData());
	}
}
