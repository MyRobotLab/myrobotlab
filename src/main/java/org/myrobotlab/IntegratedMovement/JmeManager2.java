/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.Log;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

/**
 * this class will manage the JME3 app
 * @author calamity
 *
 */
public class JmeManager2  {

	private TestJmeIMModel2 jmeApp;
	transient AssetManager assetManager;
	private HashMap<String, Node> nodes;
	private transient Queue<Node> nodeQueue = new ConcurrentLinkedQueue<Node>();
	
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

}
