/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.IntegratedMovement;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;


/**
 * this class will manage the JME3 app
 * @author calamity
 *
 */
public class JmeManager implements ActionListener {

	private JmeIMModel jmeApp;
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
	
	public JmeManager(IntegratedMovement im){
		this.im = im;
	}
	
	public void start(String string, String string2) {
	    if (jmeApp != null) {
	        //Log.info("JmeApp already started");
	        return;
	    }
	    jmeApp = new JmeIMModel();
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
			    Quaternion i = new Quaternion();
			    Quaternion q1 = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (float)ip.getYaw(), Vector3f.UNIT_Y);
			    Quaternion q2 = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (float)ip.getRoll(), Vector3f.UNIT_Z);
			    Quaternion q3 = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (float)ip.getPitch(), Vector3f.UNIT_X);
				i = q1.multLocal(q2).multLocal(q3);
				n.setLocalRotation(i);
				spatial.setLocalTranslation(Util.pointToVector3f(ip));
		    }
		    else {
		        Vector3f it = Util.pointToVector3f(Util.matrixToPoint(part.getInternTransform()));
		        Cylinder c = new Cylinder(8, 20, (float) part.getRadius(), (float) part.getLength(), true, false);
		        Geometry geom = new Geometry("Cylinder", c);
		        geom.setName(part.getName());
		        geom.setMaterial(mat);
		        Node n = new Node("n");
		        float length = (float)part.getLength();
		        if (it.getZ() < 0) length = -length;
		        geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, new Vector3f(0,0,0), new Vector3f(0, 0, length)));
		        Quaternion q = Util.matrixToQuaternion(part.getInternTransform());
		        n.setLocalRotation(q.inverse());
		        n.attachChild(geom);
		        node.attachChild(n);
		        
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
/*			Vector3f y = new Vector3f();
			y.setX(((float)o.getX()-y.getX()+(float)ini.getX()));
			y.setY(((float)o.getZ()-y.getY()+(float)ini.getY()));
			y.setZ(((float)o.getY()-y.getZ()+(float)ini.getZ()));
*/			node.setLocalTranslation(Util.pointToVector3f(Util.matrixToPoint(origin)));
			Quaternion i = Util.matrixToQuaternion(end);
			node.setLocalRotation(i);
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
