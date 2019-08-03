/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.myrobotlab.jme3.Jme3Msg;
import org.myrobotlab.jme3.Jme3Util;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.IntegratedMovement;
import org.slf4j.Logger;

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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;


/**
 * this class will manage the JME3 app
 * @author calamity
 *
 */
public class JmeManager implements ActionListener {

	transient private JmeIMModel jmeApp;
	transient AssetManager assetManager;
	transient private HashMap<String, Spatial> nodes = new HashMap<String,Spatial>();
	private long startUpdateTs;
	transient private Node rootNode;
	private long deltaMs;
	private long sleepMs;
	transient AnalogListener analog = null;
	transient InputManager inputManager;
	transient IntegratedMovement im = null;
	long frameCount =0;
	transient private MsgUtil msgUtil;
	transient private ViewPort viewPort;
	transient private Node point;
	transient private Camera cameraSetting;
	transient private CameraNode camNode;
	transient private Node camera = new Node("camera");
	public final static Logger log = LoggerFactory.getLogger(Jme3Util.class);
	protected Queue<Jme3Msg> jme3MsgQueue = new ConcurrentLinkedQueue<Jme3Msg>();
	
	public JmeManager(IntegratedMovement im){
		this.im = im;
		msgUtil = new MsgUtil(this);
	}
	
	public void start(String appName, String appType) {
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
	    Callable<String> callable = new Callable<String>() {
	    	public String call() throws Exception {
	    		System.out.println("Asynchronous Callable");
	    		return "Callable Result";
	        }
	    };
	    Future<String> future = jmeApp.enqueue(callable);
	    try {
	        future.get();

	        // default positioning
	        setTranslation(camera.getName(), 0, 3, 6);
	        cameraLookAtRoot();
	        rotateOnAxis(camera.getName(), "x", -20);
	        enableGrid(true);

	    } catch (Exception e) {
	        log.warn("future threw", e);
	    }
	    info("already started app %s", appType);
/*	    jmeApp.start();
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
*/	}

	public void enableGrid(boolean b) {
	    Spatial s = nodes.get("floor-grid");
	    if (s == null) {
	    	addGrid("floor-grid");
	    	s = get("floor-grid");
	    }
	    if (b) {
	    	s.setCullHint(CullHint.Never);
	    } else {
	    	s.setCullHint(CullHint.Always);
	    }
	}

	private void addGrid(String name) {
		addGrid(name, new Vector3f(0, -1, 0), 40, "CCCCCC");
	}

	private void addGrid(String name, Vector3f pos, int size, String color) {
	    Spatial s = nodes.get(name);
	    if (s != null) {
	      log.warn("addGrid {} already exists");
	      return;
	    }
	    Geometry g = new Geometry("wireframe grid", new Grid(size, size, 1.0f));
	    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	    mat.getAdditionalRenderState().setWireframe(true);
	    mat.setColor("Color", Jme3Util.toColor(color));
	    g.setMaterial(mat);
	    g.center().move(pos);
	    Node n = new Node(name);
	    n.attachChild(g);
	    rootNode.attachChild(n);
	    nodes.put(name, n);
	}

	private void rotateOnAxis(String name, String axis, double degrees) {
		addMsg("rotate", name, axis, degrees);
	}

	public void cameraLookAtRoot() {
		cameraLookAt(rootNode);
	}

	public void cameraLookAt(Spatial spatial) {
	    addMsg("lookAt", camera.getName(), spatial.getName());
	}

	public void info(String format, Object... params) {
		im.info(format, params);
	}

	public void setTranslation(String name, double x, double y, double z) {
		addMsg("setTranslation", name, x, y, z);
	}

	private void addMsg(String method, Object... params) {
		jme3MsgQueue.add(new Jme3Msg(method, params));
	}

	public void loadParts(IMData data) {
	    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	    mat.setColor("Color", ColorRGBA.Blue);
		for (IMPart part : data.getParts().values()){
		    Node node = new Node(part.getName());
		    String modelPath = part.get3DModelPath();
		    setVisible(part.getName(), part.isVisible());
		    if (modelPath != null) {
		        Spatial spatial = assetManager.loadModel(modelPath);
		        spatial.scale(part.getScale());
		        spatial.setName(part.getName());
		        Node theta = new Node("theta");
		        Node iniRot = new Node("iniRot");
		        Node sp = new Node("spatial");
		        Node alpha = new Node("alpha");
		        theta.attachChild(alpha);
		        alpha.attachChild(iniRot);
		        iniRot.attachChild(sp);
		        sp.attachChild(spatial);
		        
		        node.attachChild(theta);
			    Point ip = part.getInitialTranslateRotate();
			    Quaternion i = new Quaternion();
			    Quaternion q1 = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (float)ip.getYaw(), Vector3f.UNIT_Y);
			    Quaternion q2 = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (float)ip.getRoll(), Vector3f.UNIT_Z);
			    Quaternion q3 = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (float)ip.getPitch(), Vector3f.UNIT_X);
				i = q1.multLocal(q2).multLocal(q3);
				sp.setLocalRotation(i);
				spatial.setLocalTranslation(Util.pointToVector3f(ip));
				Quaternion q = Util.matrixToQuaternion(part.getInternTransform());
				iniRot.setLocalRotation(q.inverse());
		    }
		    else {
		        Cylinder c = new Cylinder(8, 20, (float) part.getRadius(), (float) part.getLength(), true, false);
		        Geometry geom = new Geometry("Cylinder", c);
		        geom.setName(part.getName());
		        geom.setMaterial(mat);
		        Node iniRot = new Node("iniRot");
		        Node theta = new Node("theta");
		        Node alpha = new Node("alpha");
			    Node origin = createUnitAxis("origin");
			    alpha.attachChild(origin);

		        float length = (float)part.getLength();
		        if (part.getR()!=0)
		        	length *=-1;
		        geom.setLocalTranslation(FastMath.interpolateLinear(0.5f, new Vector3f(0,0,0), new Vector3f(0, 0, -length)));
		        Vector3f delta = Util.pointToVector3f(Util.matrixToPoint(part.getInternTransform()));
		        delta.normalizeLocal();
		        float[] angles = new float[3];
		        angles[0]=delta.angleBetween(Vector3f.UNIT_X);
		        angles[1]=delta.angleBetween(Vector3f.UNIT_Y)+FastMath.PI;
		        angles[2]=delta.angleBetween(Vector3f.UNIT_Z)-FastMath.PI/2;
		        iniRot.rotate(-angles[0],0, 0);
		        iniRot.rotate(0,angles[1],0);
		        iniRot.rotate(0,0,angles[2]);
		        iniRot.rotate(0,0,(float)part.getInitialTheta());
		        iniRot.getLocalRotation().toAngles(angles);
		        iniRot.attachChild(geom);
		        alpha.attachChild(iniRot);
		        theta.attachChild(alpha);
		        node.attachChild(theta);
		        
		    }
		    nodes.put(part.getName(), node);
		    addMsg("addNode", node.getName());
		}
	}

	public void simpleUpdate(float tpf) {
	    // start the clock on how much time we will take
		startUpdateTs = System.currentTimeMillis();
	    while (jme3MsgQueue.size() > 0) {
	        Jme3Msg msg = null;
	        try {
	          msg = jme3MsgQueue.remove();
	          msgUtil.invoke(msg);
	        } catch (Exception e) {
	          log.error("simpleUpdate failed for {} - targetName", msg, e);
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
			Node node = (Node)nodes.get(part.getName());
			if (node == null) continue;
			Matrix origin = part.getOrigin();
			node.setLocalTranslation(Util.pointToVector3f(Util.matrixToPoint(origin)));
			Quaternion q = Util.matrixToQuaternion(origin);
			Vector3f[] axis = new Vector3f[3];
			q.toAxes(axis);
			float[] angles = new float[3];
			q.toAngles(angles);
			Quaternion q3 = new Quaternion().fromAngles(0, (float)(part.getTheta()),0);
			Spatial n1 = node.getChild("theta");
			n1.setLocalRotation(q3);
			Quaternion i = Util.matrixToQuaternion(origin);
			node.setLocalRotation(i);
		}
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
	    log.info("onAction {} {} {}", name, isPressed, tpf);
		
	}

	public void simpleInitApp() {
		assetManager = jmeApp.getAssetManager();
		assetManager.registerLocator("inmoov/jm3/assets", FileLocator.class);
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Green);
		Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat2.setColor("Color", ColorRGBA.Blue);
		viewPort = jmeApp.getViewPort();
		viewPort.setBackgroundColor(ColorRGBA.Gray);
		inputManager = jmeApp.getInputManager();
		inputManager.setCursorVisible(true);
		jmeApp.getFlyByCamera().setEnabled(false);
		rootNode = jmeApp.getRootNode();
		loadParts(im.getData());
	    Cylinder c = new Cylinder(8, 50, .005f, .010f, true, false);
	    Geometry geom = new Geometry("Cylinder", c);
	    geom.setMaterial(mat);
	    point = new Node("point");
	    point.attachChild(geom);
	    rootNode.attachChild(point);
	    Cylinder c2 = new Cylinder(8, 50, .005f, .010f, true, false);
	    Geometry geom2 = new Geometry("Cylinder", c2);
	    geom2.setMaterial(mat2);
	    Node point2 = new Node("point");
	    point2.attachChild(geom2);
	    point2.setLocalTranslation(0.3f, 0,  0f);
	    rootNode.attachChild(point2);
	    DirectionalLight sun = new DirectionalLight();
	    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
	    rootNode.addLight(sun);
	    nodes.put(rootNode.getName(), rootNode);
	    cameraSetting = jmeApp.getCamera();
	    cameraSetting.setLocation(new Vector3f(0f, 0f, 3f));
	    camNode = new CameraNode("cam", cameraSetting);
	    camNode.setControlDir(ControlDirection.SpatialToCamera);
	    camera.attachChild(camNode);
	    nodes.put(camera.getName(), camera);
	    inputManager.addMapping("MouseClickL", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
	    inputManager.addListener(analogListener, "MouseClickL");
	    inputManager.addMapping("MouseClickR", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
	    inputManager.addListener(analogListener, "MouseClickR");
	    inputManager.addMapping("MMouseUp", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
	    inputManager.addListener(analogListener, "MMouseUp");
	    inputManager.addMapping("MMouseDown", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
	    inputManager.addListener(analogListener, "MMouseDown");
	    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT)); // A
	    // and
	    // left
	    // arrow
	    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT)); // D
	    // and
	    // right
	    // arrow
	    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP)); // A
	    // and
	    // left
	    // arrow
	    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN)); // D
	    // and
	    // right
	    // arrow
	    inputManager.addListener(analogListener, new String[] { "Left", "Right", "Up", "Down" });
	}
	private AnalogListener analogListener = new AnalogListener() {
		public void onAnalog(String name, float keyPressed, float tpf) {
			if (name.equals("MouseClickL")) {
		        rootNode.rotate(0, -keyPressed, 0);
		      } else if (name.equals("MouseClickR")) {
		        rootNode.rotate(0, keyPressed, 0);
		      } else if (name.equals("MMouseUp")) {
		        rootNode.setLocalScale(rootNode.getLocalScale().mult(1.05f));
		      } else if (name.equals("MMouseDown")) {
		        rootNode.setLocalScale(rootNode.getLocalScale().mult(0.95f));
		      } else if (name.equals("Up")) {
		        rootNode.move(0, -keyPressed * 0.300f, 0);
		      } else if (name.equals("Down")) {
		        rootNode.move(0, keyPressed * 0.300f, 0);
		      } else if (name.equals("Left")) {
		        rootNode.move(-keyPressed * 0.100f, 0, 0);
		      } else if (name.equals("Right")) {
		        rootNode.move(keyPressed * 0.100f, 0, 0);
		      }
		  }
	};

	public Spatial get(String name) {
		return nodes.get(name);
	}

	public Node getRootNode() {
		return rootNode;
	}
	
	  public Node createUnitAxis(String name) {

		    Node n = new Node(name);
		    Arrow arrow = new Arrow(Vector3f.UNIT_X);
		    n.setLocalScale(0.2f);
		    n.attachChild(createAxis("x", arrow, ColorRGBA.Red));

		    arrow = new Arrow(Vector3f.UNIT_Y);
		    n.attachChild(createAxis("y", arrow, ColorRGBA.Green));

		    arrow = new Arrow(Vector3f.UNIT_Z);
		    n.attachChild(createAxis("z", arrow, ColorRGBA.Blue));
		    return n;
		  }

	  public Geometry createAxis(String name, Mesh shape, ColorRGBA color) {
		    Geometry g = new Geometry(name, shape);
		    Material mat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		    mat.getAdditionalRenderState().setWireframe(true);
		    mat.getAdditionalRenderState().setLineWidth(1);
		    mat.setColor("Color", color);
		    g.setMaterial(mat);
		    return g;
		  }

	public void setVisible(String name, boolean visible) {
		addMsg("setVisible", name, visible);
	}
	
	public void setAxesVisible(boolean b){
		addMsg("setAxesVisible", b);
	}

	public HashMap<String, Spatial> getNodes() {
		return nodes;
	}
	
	public void setAxesVisible(String name, boolean b){
		addMsg("setAxesVisible", name, b);
	}

}
