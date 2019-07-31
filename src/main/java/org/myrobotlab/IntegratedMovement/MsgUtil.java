/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

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
		//Spatial s = jme.get(name);
		//s.setLocalTranslation((float)x, (float)y, (float)z);
	}

}
