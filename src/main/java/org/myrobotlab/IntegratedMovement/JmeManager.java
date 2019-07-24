/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.jme3.HudText;
import org.myrobotlab.jme3.Jme3App;
import org.myrobotlab.jme3.Jme3Msg;
import org.myrobotlab.jme3.Jme3Util;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.Runtime;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.collision.CollisionResults;
import com.jme3.export.binary.BinaryExporter;
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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.debug.Grid;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;

/**
 * this class will manage the JME3 app
 * @author calamity
 *
 */
public class JmeManager implements ActionListener {
	private JmeIMModel app;
	private AppSettings settings;
	private int width = 1024;
	private int height = 768;
	transient Node rootNode;
	transient AssetManager assetManager;
	protected Queue<Jme3Msg> jme3MsgQueue = new ConcurrentLinkedQueue<Jme3Msg>();
	transient AppStateManager stateManager;
	transient InputManager inputManager;
	transient Node guiNode;
	transient FlyByCamera flyCam;
	transient Camera cameraSettings;
	transient Node camera = new Node(CAMERA);
	transient ViewPort viewPort;
	transient CameraNode camNode;
	transient AnalogListener analog = null;
	boolean mouseRightPressed = false;
	boolean shiftLeftPressed = false;
	boolean ctrlLeftPressed = false;
	boolean altLeftPressed = false;
	transient Spatial selectedForView = null;
	boolean mouseLeftPressed = false;
	int selectIndex = 0;
	transient Jme3Util util;
	boolean fullscreen = false;
	transient DisplayMode displayMode = null;
	private String name;
	long startUpdateTs;
	boolean saveHistory = false;
	transient Map<String, HudText> guiText = new TreeMap<>();
	List<Jme3Msg> history = new ArrayList<Jme3Msg>();
	long deltaMs;
	long sleepMs;
	private HashMap<String, Node> nodes = new HashMap<String,Node>();
	private transient Queue<Node> nodeQueue = new ConcurrentLinkedQueue<Node>();

	final static String CAMERA = "camera";
	final static String ROOT = "root";



	public void simpleUpdate(float tpf) {

		    // start the clock on how much time we will take
		startUpdateTs = System.currentTimeMillis();

		for (HudText hudTxt : guiText.values()) {
		    hudTxt.update();
		}

		//interpolator.generateMoves();

		while (jme3MsgQueue.size() > 0) {
			Jme3Msg msg = null;
		    try {

		        // TODO - support relative & absolute moves
		    	msg = jme3MsgQueue.remove();
		        if (saveHistory) {
		        	history.add(msg);
		        }
		        util.invoke(msg);
		    } catch (Exception e) {
		    //    log.error("simpleUpdate failed for {} - targetName", msg, e);
		    }
		}
	    while (nodeQueue.size() > 0) {
	        Node node = nodeQueue.remove();
	        Node hookNode = rootNode;
	        hookNode.attachChild(node);
	        Spatial x = hookNode.getChild(node.getName());
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
	}
	
	synchronized public SimpleApplication start(String appName, String appType, IMData data) {
		name = appName;
		if (Runtime.isHeadless()) {
		    return null;
		}
		    
		if (app == null) {
		// create app

		    app = new JmeIMModel(this);

		      // start it with "default" settings
		    settings = new AppSettings(true);
		    settings.setResolution(width, height);
		    // settings.setEmulateMouse(false);
		    // settings.setUseJoysticks(false);
		    settings.setUseInput(true);
		    settings.setAudioRenderer(null);
		    app.setSettings(settings);
		    app.setShowSettings(true); // resolution bps etc dialog
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

		        // default positioning
		        moveTo(CAMERA, 0, 3, 6);
		        cameraLookAtRoot();
		        rotateOnAxis(CAMERA, "x", -30);
		        enableGrid(true);

		    } catch (Exception e) {
		        //log.warn("future threw", e);
		    }
		    return app;
	    }
		    //info("already started app %s", appType);

	    return app;
	}

	public void loadParts(IMData data) {
		for (IMPart part : data.getParts().values()){
		    Node node = new Node(part.getName());
		    String modelPath = part.get3DModelPath();
		    if (modelPath != null) {
		        Spatial spatial = assetManager.loadModel(modelPath);
		        spatial.scale(part.getScale());
		        spatial.setName(part.getName());
		        node.attachChild(spatial);
		    }
		    Point ip = part.getInitialTranslateRotate();
		    node.setLocalTranslation((float)ip.getX()*part.getScale(), (float)ip.getY()*part.getScale(), (float)ip.getZ()*part.getScale());
		    node.setLocalRotation(FKinematics.eulerToMatrix(ip));
		    nodes.put(part.getName(), node);
		    nodeQueue.add(node);
		}
		
	}

	public void enableGrid(boolean b) {
	    Spatial s = find("floor-grid");
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

	public Spatial get(String name) {
	    return get(name, null);
	}

	public Spatial get(String name, Node startNode) {
	    Spatial ret = find(name, startNode);
	    if (ret == null) {
	      //error("get(%s) could not find child", name);
	    }
	    return ret;
	}

	public Spatial find(String name, Node startNode) {
	    if (name.equals(ROOT)) {
	        return rootNode;
	      }
	      if (startNode == null) {
	        startNode = rootNode;
	      }

	      Spatial child = startNode.getChild(name);
	      return child;
	}

	public void addGrid(String name) {
		addGrid(name, new Vector3f(0, 0, 0), 40, "CCCCCC");
	}

	public void addGrid(String name, Vector3f pos, int size, String color) {
		Spatial s = find(name);
		if (s != null) {
			//log.warn("addGrid {} already exists");
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
	}

	public Spatial find(String name) {
		return find(name, null);
	}

	public void rotateOnAxis(String name, String axis, double degrees) {
		addMsg("rotateTo", name, axis, degrees);
	}

	public void addMsg(String method, Object... params) {
		jme3MsgQueue.add(new Jme3Msg(method, params));
	}

	public void cameraLookAtRoot() {
		cameraLookAt(rootNode);
	}

	public void cameraLookAt(Spatial spatial) {

		    // INTERESTING BUG - DO NOT DIRECTLY LOOK AT BECAUSE WHEN WE PUT COMMANDS IN
		    // ORDER
		    // ROTATING (to lookAt) IS NOT TRANSITIVE, AND THIS HAPPENS BEFORE ANY
		    // PREVIOUS MOVE :P
		    // SO IT DOES NOT WORK - solution is to process the lookAt with the JME
		    // thread processing
		    // all the other moves & rotations !
		    // camera.lookAt(spatial.getWorldTranslation(), Vector3f.UNIT_Y);
		addMsg("lookAt", CAMERA, spatial.getName());
	}

	public void moveTo(String name, double x, double y, double z) {
		setTranslation(name, x, y, z);
	}

	public void setTranslation(String name, double x, double y, double z) {
		addMsg("setTranslation", name, x, y, z);
	}

	public void simpleInitApp() {

	    stateManager = app.getStateManager();
	    setDisplayFps(true);

	    setDisplayStatView(true);
	    // wtf - assetManager == null - another race condition ?!?!?
	    // after start - these are initialized as "default"
	    assetManager = app.getAssetManager();
	    inputManager = app.getInputManager();

	    guiNode = app.getGuiNode();
	    // Initialize the globals access so that the default
	    // components can find what they need.
	    GuiGlobals.initialize(app);
	    // Load the 'glass' style
	    BaseStyles.loadGlassStyle();
	    // Set 'glass' as the default style when not specified
	    GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

	    // disable flycam we are going to use our
	    // own camera
	    flyCam = app.getFlyByCamera();
	    if (flyCam != null) {
	      flyCam.setEnabled(false);
	    }

	    cameraSettings = app.getCamera();
	    rootNode = app.getRootNode();
	    rootNode.setName(ROOT);
	    rootNode.attachChild(camera);

	    viewPort = app.getViewPort();
	    // Setting the direction to Spatial to camera, this means the camera will
	    // copy the movements of the Node
	    camNode = new CameraNode("cam", cameraSettings);
	    camNode.setControlDir(ControlDirection.SpatialToCamera);
	    camera.attachChild(camNode);
	    inputManager.setCursorVisible(true);

	    // FIXME - should be moved under ./data/JMonkeyEngine/
	    assetManager.registerLocator("InMoov/jm3/assets", FileLocator.class);

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

	    // wheelmouse zoom (check)
	    // alt+ctrl+lmb - zoom <br>
	    // alt+lmb - rotate<br>
	    // alt+shft+lmb - pan
	    // rotate around selection -
	    // https://www.youtube.com/watch?v=IVZPm9HAMD4&feature=youtu.be
	    // wrap text of breadcrumbs
	    // draggable - resize for menu - what you set is how it stays
	    // when menu active - inputs(hotkey when non-menu) should be deactive

	    inputManager.addMapping("mouse-click-left", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
	    inputManager.addListener(this, "mouse-click-left");

	    inputManager.addMapping("mouse-click-right", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
	    inputManager.addListener(this, "mouse-click-right");

	    inputManager.addMapping("mouse-wheel-up", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
	    inputManager.addListener(analog, "mouse-wheel-up");
	    inputManager.addMapping("mouse-wheel-down", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
	    inputManager.addListener(analog, "mouse-wheel-down");

	    inputManager.addMapping("mouse-axis-x", new MouseAxisTrigger(MouseInput.AXIS_X, true));
	    inputManager.addListener(analog, "mouse-axis-x");

	    inputManager.addMapping("mouse-axis-x-negative", new MouseAxisTrigger(MouseInput.AXIS_X, false));
	    inputManager.addListener(analog, "mouse-axis-x-negative");

	    inputManager.addMapping("mouse-axis-y", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
	    inputManager.addListener(analog, "mouse-axis-y");

	    inputManager.addMapping("mouse-axis-y-negative", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
	    inputManager.addListener(analog, "mouse-axis-y-negative");

	    inputManager.addMapping("select-root", new KeyTrigger(KeyInput.KEY_R));
	    inputManager.addListener(this, "select-root");

	    inputManager.addMapping(CAMERA, new KeyTrigger(KeyInput.KEY_C));
	    inputManager.addListener(this, CAMERA);

	    inputManager.addMapping("menu", new KeyTrigger(KeyInput.KEY_M));
	    inputManager.addListener(this, "menu");
	    inputManager.addMapping("full-screen", new KeyTrigger(KeyInput.KEY_F));
	    inputManager.addListener(this, "full-screen");
	    inputManager.addMapping("exit-full-screen", new KeyTrigger(KeyInput.KEY_G));
	    inputManager.addListener(this, "exit-full-screen");
	    inputManager.addMapping("cycle", new KeyTrigger(KeyInput.KEY_TAB));
	    inputManager.addListener(this, "cycle");
	    inputManager.addMapping("shift-left", new KeyTrigger(KeyInput.KEY_LSHIFT));
	    inputManager.addListener(this, "shift-left");
	    inputManager.addMapping("ctrl-left", new KeyTrigger(KeyInput.KEY_LCONTROL));
	    inputManager.addListener(this, "ctrl-left");
	    inputManager.addMapping("alt-left", new KeyTrigger(KeyInput.KEY_LMENU));
	    inputManager.addListener(this, "alt-left");
	    inputManager.addMapping("export", new KeyTrigger(KeyInput.KEY_E));
	    inputManager.addListener(this, "export");

	    viewPort.setBackgroundColor(ColorRGBA.Gray);

	    DirectionalLight sun = new DirectionalLight();
	    sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));

	    rootNode.addLight(sun);

	    rootNode.addLight(sun);
	    rootNode.setLocalTranslation(0, 0, 0);
	  }


	public void setDisplayStatView(boolean b) {
		app.setDisplayStatView(b);
	}

	public void setDisplayFps(boolean b) {
		app.setDisplayFps(b);
	}


	@Override
	public void onAction(String name, boolean keyPressed, float tpf) {
	    //log.info("onAction {} {} {}", name, keyPressed, tpf);

		if (name.equals("mouse-click-right")) {
			mouseRightPressed = keyPressed;
			if (mouseRightPressed) {
				Geometry target = checkCollision();
				setSelected(target);
			}
	    }

	    if ("full-screen".equals(name)) {
	    	enableFullScreen(true);
//	    } else if ("menu".equals(name)) {
//	    	menu.setEnabled(true);
	    } else if ("select-root".equals(name)) {
	    	setSelected(rootNode);
	    } else if (CAMERA.equals(name)) {
	    	setSelected(CAMERA);
	    } else if ("exit-full-screen".equals(name)) {
	    	enableFullScreen(false);
	    } else if ("cycle".equals(name) && keyPressed) {
	    	cycle();
	    } else if (name.equals("shift-left")) {
	    	shiftLeftPressed = keyPressed;
	    } else if (name.equals("ctrl-left")) {
	    	ctrlLeftPressed = keyPressed;
	    } else if (name.equals("alt-left")) {
	    	altLeftPressed = keyPressed;
	    } else if ("export".equals(name) && keyPressed) {
	    	saveSpatial(selectedForView.getName());
	    } else if ("mouse-click-left".equals(name)) {
	    	mouseLeftPressed = keyPressed;
	    } else {
	    	//warn("%s - key %b %f not found", name, keyPressed, tpf);
	    }
	}


	public boolean saveSpatial(String name) {
		Spatial spatial = get(name);
		return saveSpatial(spatial, spatial.getName());
	}


	 // FIXME - fix name - because it can save a Geometry too
	public boolean saveSpatial(Spatial spatial, String filename) {
		try {

			if (spatial == null) {
				//error("cannot save null spatial");
				return false;
			}

			String name = spatial.getName();

			if (filename == null) {
				filename = name + ".j3o";
			}

			filename = FileIO.cleanFileName(filename);
			BinaryExporter exporter = BinaryExporter.getInstance();
			FileOutputStream out = new FileOutputStream(filename);
			exporter.save(spatial, out);
			out.close();

			return true;
	    } catch (Exception e) {
	      //log.error("exporter.save threw", e);
	    }
	    return false;
	}


	/**
	 * cycles through children at same level
	*/
	public void cycle() {

	    if (selectedForView == null) {
	    	Spatial s = rootNode.getChild(0);
	    	setSelected(s);
	    }

	    Node parent = selectedForView.getParent();
	    if (parent == null) {
	    	return;
	    }

	    List<Spatial> siblings = parent.getChildren();

	    if (shiftLeftPressed) {
	    	--selectIndex;
	    } else {
	    	++selectIndex;
	    }

	    if (selectIndex > siblings.size() - 1) {
	    	selectIndex = 0;
	    } else if (selectIndex < 0) {
	    	selectIndex = siblings.size() - 1;
	    }

	    setSelected(siblings.get(selectIndex));
	}


	public void setSelected(Spatial newSelected) {

		    // turn off old
		if (selectedForView != null) {
			enableBoundingBox(selectedForView, false);
		    enableAxes(selectedForView, false);
		}

		    // set selected
		selectedForView = newSelected;

		    // send the movement utility info on the current selected item & current
		    // view
		    // so that it can update the view with changes on the item
		    // TODO - optimize for when there is no view
		//util.setSelectedForView(menu, selectedForView);

		    // display in menu
		//menu.putText(newSelected);

		    // turn on new
		if (newSelected != null) {
			enableBoundingBox(newSelected, true);
		    enableAxes(newSelected, true);
		}
	}


	  private void enableBoundingBox(Spatial selectedForView2, boolean b) {
		// TODO Auto-generated method stub
		
	}


	// FIXME !!!! enableCoodinateAxes - same s bb including parent if geometry
	public void enableAxes(Spatial spatial, boolean b) {

	    /**
	     * mmm - may be a bad idea - but may need to figure solution out.. if
	     * (spatial instanceof Geometry) { UserData data =
	     * jme.getUserData(spatial.getParent()); data.enableCoordinateAxes(b);
	     * return; }
	     */

		if (spatial.getName().startsWith("_")) {
			//log.warn("enableAxes(%s) a meta object not creating/enabling", spatial.getName());
			return;
	    }

	    // we need the geometry's parent
	    Node parent = spatial.getParent();
	    // we need to check to see if this uniquely named Geometry's bb exists ..
	    String axesName = getCoorAxesName(spatial); //
	    Spatial axis = find(axesName, parent);
	    if (axis == null) {
	    	axis = createUnitAxis(axesName);
	    }

	    if (spatial instanceof Geometry) {
	    	parent.attachChild(axis);
	    } else {
	      // spatial is a node - attach it directly
	    	((Node) spatial).attachChild(axis);
	    }
	    if (b) {
	    	axis.setCullHint(CullHint.Never);
	    } else {
	    	axis.setCullHint(CullHint.Always);
	    }
	}


	public Node createUnitAxis(String name) {
	    return util.createUnitAxis(name);
	}


	public String getCoorAxesName(Spatial spatial) {
	    if (spatial.getName().startsWith("_")) {
	    	return null;
		}
		String geoBbName = String.format("_axis-%s-%s", getType(spatial), spatial.getName());
		return geoBbName;
	}


	public String getType(Spatial spatial) {
	    if (spatial instanceof Node) {
	    	return "n";
		} else {
		    return "g";
		}
	}


	public void enableBoundingBox(Spatial spatial, boolean b, String color) {
		if (spatial == null) {
			//log.error("enableBoundingBox(null) - spatial cannot be null");
		    return;
		}

		String name = spatial.getName();

		if (name.startsWith("_")) {
		//     log.warn("enableBoundingBox(%s) begins with \"_\" is a meta node - will not create new bounding box", name);
		      // might not be desirable to simply return - might need to "turn off" an
		      // existing bounding box
			return;
		}

		if (color == null) {
			color = Jme3Util.defaultColor;
		}

		    // we need the geometry's parent
		Node parent = spatial.getParent();
		    // we need to check to see if this uniquely named Geometry's bb exists ..
		String geoBbName = getBbName(spatial); //
		Spatial bb = find(geoBbName, parent);
		if (bb == null) {
			bb = createBoundingBox(spatial, color);
		    if (bb == null) {
		        //log.info("bb for {} could not be created", spatial.getName());
		        return;
		    }
		}
		    // now we have the bb

		    // BB is a "new" object - and you can't add nodes to a Geometry,
		    // so current strategy is to grab the Geometry's parent and add
		    // a name "unique" BB for that Geometry

		if (spatial instanceof Geometry) {
			parent.attachChild(bb);
		} else {
		      // spatial is a node - attach it directly
			((Node) spatial).attachChild(bb);
		}

		    // FIXME !!! - so it turns out scale is correct if NOT attached to the
		    // node/tree system which has been scaled :P
		    // the following gives an accurately sized bounding box - BUT it will not
		    // move with the node in question :(
		    // rootNode.attachChild(bb);

		if (b) {
		    bb.setCullHint(CullHint.Never);
		} else {
		    bb.setCullHint(CullHint.Always);
		}
	}


	public Geometry createBoundingBox(Spatial spatial, String color) {
		return util.createBoundingBox(spatial, color);
	}


	public String getBbName(Spatial spatial) {
		if (spatial.getName().startsWith("_")) {
		    return null;
		}
		String geoBbName = String.format("_bb-%s-%s", getType(spatial), spatial.getName());
		return geoBbName;
	}


	public void setSelected(String name) {
		Spatial s = get(name);
		if (s == null) {
		//      log.error("setSelected {} is null", name);
		  	return;
		}
		setSelected(s);
	}


	public void enableFullScreen(boolean fullscreen) {
		this.fullscreen = fullscreen;

		if (fullscreen) {
			GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		    displayMode = device.getDisplayMode();
		      // remember last display mode
		    displayMode = device.getDisplayMode();

		    settings = app.getContext().getSettings();
		    //  log.info("settings {}", settings);
		    settings.setTitle(name);
		    settings.setResolution(displayMode.getWidth(), displayMode.getHeight());
		    settings.setFrequency(displayMode.getRefreshRate());
		    settings.setBitsPerPixel(displayMode.getBitDepth());
		    settings.setFullscreen(fullscreen);
		    app.setSettings(settings);
		    app.restart();
		} else {
		      settings = app.getContext().getSettings();
		//      log.info("settings {}", settings);
		      settings.setFullscreen(fullscreen);
		      settings.setResolution(width, height);
		      app.setSettings(settings);
		      app.restart();
		}
	}



	 // FIXME make a more general Collision check..
	public Geometry checkCollision() {

	    // Reset results list.
	    CollisionResults results = new CollisionResults();
	    // Convert screen click to 3d position
	    Vector2f click2d = inputManager.getCursorPosition();
	    Vector3f click3d = cameraSettings.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
	    Vector3f dir = cameraSettings.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
	    // Aim the ray from the clicked spot forwards.
	    Ray ray = new Ray(click3d, dir);
	    // Collect intersections between ray and all nodes in results list.
	    rootNode.collideWith(ray, results);
	    // (Print the results so we see what is going on:)
	    for (int i = 0; i < results.size(); i++) {
	      // (For each “hit”, we know distance, impact point, geometry.)
	    	float dist = results.getCollision(i).getDistance();
	    	Vector3f pt = results.getCollision(i).getContactPoint();
	    	String target = results.getCollision(i).getGeometry().getName();
	    	System.out.println("Selection #" + i + ": " + target + " at " + pt + ", " + dist + " WU away.");
	    }
	    // Use the results -- we rotate the selected geometry.
	    if (results.size() > 0) {
	      // The closest result is the target that the player picked:
	    	Geometry target = results.getClosestCollision().getGeometry();
	      // Here comes the action:
	      //log.info("you clicked " + target.getName());
	    	return target;
	    }
	    return null;
	  }

}
