/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import org.myrobotlab.jme3.Jme3Msg;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;

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
	
	public void setVisible(String nodeName, boolean b){
		Spatial node = jme.get(nodeName);
		if (b){
			node.setCullHint(CullHint.Never);
		}
		else{
			node.setCullHint(CullHint.Always);
		}
	}
	
	public void setAxesVisible(boolean b){
		CullHint visible = CullHint.Never;
		if (!b) visible = CullHint.Always;
		for (Spatial s : jme.getNodes().values()){
			Spatial origin = ((Node)s).getChild("origin");
			if (origin != null) origin.setCullHint(visible);
		}
	}
	
	public void setAxesVisible(String name, boolean b){
		Node node = (Node)jme.get(name);
		if (node == null){
			JmeManager.log.error("No node named {} in setAxesVisible", name);
			return;
		}
		Node origin = (Node)node.getChild("origin");
		if (origin == null){
			JmeManager.log.error("Node {} do not contain axes info in setAxesVisible", name);
			return;
		}
		CullHint visible = CullHint.Never;
		if (!b) visible = CullHint.Always;
		origin.setCullHint(visible);
	}
}
