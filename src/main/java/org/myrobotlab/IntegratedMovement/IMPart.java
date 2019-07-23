/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

/**
 * Contain Info about a part or object that can be used by IntegratedMovement
 * @author calamity
 *
 */
public class IMPart {

	
	private String name;
	private HashMap<String,String> controls = new HashMap<String, String>();
	private HashMap<String,DHLink> DHLinks = new HashMap<String, DHLink>();
	private HashMap<String, ServoStatus> states = new HashMap<String, ServoStatus>();

	public IMPart(String partName){
		name = partName;
	}

	

	public String getName() {
		return name;
	}



	public void setControl(String armModel, String control) {
		controls.put(armModel, control);
	}



	public void setDHParameters(String armModel, int d, int theta, int r, int alpha) {
		DHLink link = new DHLink(name, d, theta, r, alpha);
		DHLinks.put(armModel, link);
	}



	public String getControl(String armName) {
		return controls.get(armName);
	}



	public void setState(String armName, ServoStatus state) {
		states.put(armName,state);
		
	}



	public void setSpeed(String arm, Double speed) {
		// TODO Auto-generated method stub
		
	}



	public HashMap<String, String> getControls() {
		return controls;
	}
}
