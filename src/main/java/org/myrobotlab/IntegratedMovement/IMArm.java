/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.LinkedList;

/**
 * @author calamity
 *
 */
public class IMArm {
	
	String name;
	LinkedList<IMPart> parts = new LinkedList<IMPart>();
	
	public IMArm(String name){
		this.name = name;
	}

	public void add(IMPart part) {
		parts.add(part);
	}
	
	public void addFirst(IMPart part){
		parts.addFirst(part);
	}

}
