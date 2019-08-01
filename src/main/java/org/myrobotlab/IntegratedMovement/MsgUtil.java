/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import org.myrobotlab.jme3.Jme3Msg;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * @author calamity
 *
 */
public class MsgUtil {
	
	transient private JmeManager jme;
	
	MsgUtil(JmeManager app) {
		jme = app;
	}

	public void setTranslation(String name, double x, double y, double z){
	    JmeManager.log.info(String.format("setTranslation %s, %.2f,%.2f,%.2f", name, x, y, z));
	    Spatial s = jme.get(name);
	    s.setLocalTranslation((float) x, (float) y, (float) z);
	}
	
	public Object invoke(Jme3Msg msg) {
		return jme.im.invokeOn(this, msg.method, msg.data);
	}
	
	public void lookAt(String viewer, String viewee) {
		JmeManager.log.info("lookAt({}, {})", viewer, viewee);
		Spatial viewerSpatial = jme.get(viewer);
		Spatial vieweeSpatial = jme.get(viewee);
		if (viewerSpatial == null) {
		    JmeManager.log.error("could not find {}", viewer);
		    return;
		}
		if (vieweeSpatial == null) {
		    JmeManager.log.error("could not find {}", viewee);
		    return;
		}
		viewerSpatial.lookAt(vieweeSpatial.getWorldTranslation(), Vector3f.UNIT_Y);
	}
	
	public void rotate(String name, String axis, double degree){
		Spatial s = jme.get(name);
		char[] c = axis.toCharArray();
		for (char ch : c){
			if (ch == 'x') s.rotate((float)degree, 0, 0);
			else if (ch == 'y') s.rotate(0, (float)degree, 0);
			else if (ch == 'z') s.rotate(0, 0, (float)degree);
		}
	}
	
	public void addNode(String nodeName){
	    Node rootNode = jme.getRootNode();
	    Node pivot = new Node("pivot");
	    rootNode.attachChild(pivot);
	    Node node = (Node)jme.get(nodeName);
	    pivot.attachChild(node);
	    Spatial x = rootNode.getChild(node.getName());
	    if (x != null) {
	      rootNode.updateGeometricState();
	    }
	}

}
